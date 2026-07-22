package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceSerial
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.converter.DeviceConverter
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.device.repository.DeviceSerialRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.util.generateDeviceSerial
import com.reborn.server.global.util.generateRandomCode
import com.reborn.server.global.util.generateUuid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class DeviceService(
    private val placeRepository: PlaceRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceSerialRepository: DeviceSerialRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val redisUtil: RedisUtil,
    @param:Value("\${operator.api-key:}") private val operatorApiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(userId: Long, request: DeviceDto.RegisterRequest): DeviceDto.RegisterResponse {
        val placeId = request.placeId
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "장소 ID는 필수입니다.")
        val serial = request.deviceId?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "시리얼 번호는 필수입니다.")
        val deviceName = request.deviceName?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 이름은 필수입니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        requireAdmin(userId, placeId)

        // #147: deviceKey는 더 이상 관리자가 임의로 정하는 값이 아니라, 판매 전 서비스 운영자가
        // 미리 발급해 실물에 부착한 시리얼이다 — device_serial 재고에 있는지, 아직 미할당인지 확인하고
        // 그 row에 박힌 deviceType으로 기기를 만든다(클라이언트가 보내는 타입은 신뢰하지 않음).
        val deviceSerial = deviceSerialRepository.findBySerial(serial)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 시리얼 번호입니다.")
        if (deviceSerial.assignedDevice != null) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        val device = try {
            deviceRepository.save(
                Device(place = place, deviceType = deviceSerial.deviceType, deviceKey = serial, name = deviceName),
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("register 기기 저장 중 무결성 위반: {}", e.message)
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }
        deviceSerial.assignTo(device)
        deviceSerialRepository.save(deviceSerial)

        return DeviceConverter.toRegisterResponse(device)
    }

    // #147: 판매 전 서비스 운영자가 실물에 인쇄할 시리얼을 배치로 미리 발급한다. 장소/ADMIN과 무관한
    // 서비스 전체 운영 작업이라 장소별 권한 체계 대신 정적 운영자 키(X-Operator-Key)로 인가한다.
    @Transactional
    fun generateSerialBatch(operatorKey: String, deviceType: DeviceType, count: Int): DeviceDto.SerialBatchResponse {
        requireOperator(operatorKey)

        val prefix = deviceType.serialPrefix
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "시리얼 발급 대상이 아닌 기기 유형입니다.")
        if (count !in 1..MAX_SERIAL_BATCH_COUNT) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "발급 개수는 1~${MAX_SERIAL_BATCH_COUNT}개여야 합니다.")
        }

        val serials = (1..count).map { reserveUniqueSerial(prefix, deviceType) }
        return DeviceDto.SerialBatchResponse(serials = serials)
    }

    fun generatePairingCode(userId: Long, placeId: Long): DeviceDto.PairingCodeResponse {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        requireAdmin(userId, placeId)

        val code = reserveUniqueCode(PAIRING_PREFIX, PAIRING_CODE_LENGTH, placeId.toString())

        return DeviceDto.PairingCodeResponse(
            pairingCode = code,
            expiresAt = LocalDateTime.now().plusMinutes(PAIRING_TTL_MINUTES),
        )
    }

    @Transactional
    fun pairDevice(request: DeviceDto.PairingRequest): DeviceDto.PairingResponse {
        val code = request.pairingCode?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "페어링 코드는 필수입니다.")
        val deviceName = request.deviceName?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 이름은 필수입니다.")

        val redisKey = "$PAIRING_PREFIX$code"
        val placeId = redisUtil.getAndDelete(redisKey)?.toLongOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "페어링 코드가 만료되었거나 유효하지 않습니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        val appToken = generateUuid()
        val device = try {
            deviceRepository.save(
                Device(
                    place = place,
                    deviceType = DeviceType.AEROMETER,
                    deviceKey = generateUuid(),
                    name = deviceName,
                    appToken = appToken,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("pairDevice 기기 저장 중 무결성 위반: {}", e.message)
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        return DeviceDto.PairingResponse(deviceId = device.deviceKey, placeId = place.id, appToken = appToken)
    }

    fun getList(userId: Long, placeId: Long): DeviceDto.ListResponse {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        requireAdmin(userId, placeId)

        val devices = deviceRepository.findAllByPlaceId(placeId).map { DeviceConverter.toDeviceItem(it) }
        return DeviceDto.ListResponse(devices = devices)
    }

    private fun requireAdmin(userId: Long, placeId: Long) {
        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
        if (mapping == null || mapping.accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }

    // Gemini API 키(GeminiClient.requireConfigured)와 동일한 fail-closed 패턴 — 운영자 키를
    // 설정하지 않으면 누구도 시리얼을 발급할 수 없다.
    private fun requireOperator(operatorKey: String) {
        if (operatorApiKey.isBlank() || operatorKey != operatorApiKey) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "운영자 권한이 없습니다.")
        }
    }

    private fun reserveUniqueSerial(prefix: String, deviceType: DeviceType): String {
        repeat(MAX_SERIAL_GENERATION_ATTEMPTS) {
            val serial = generateDeviceSerial(prefix)
            try {
                deviceSerialRepository.save(DeviceSerial(serial = serial, deviceType = deviceType))
                return serial
            } catch (e: DataIntegrityViolationException) {
                log.warn("시리얼 생성 중 충돌, 재시도: {}", e.message)
            }
        }
        throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "시리얼 생성에 실패했습니다. 다시 시도해주세요.")
    }

    private fun reserveUniqueCode(prefix: String, length: Int, value: String): String {
        repeat(MAX_CODE_GENERATION_ATTEMPTS) {
            val code = generateRandomCode(length)
            if (redisUtil.setIfAbsent("$prefix$code", value, Duration.ofMinutes(PAIRING_TTL_MINUTES))) {
                return code
            }
        }
        throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "코드 생성에 실패했습니다. 다시 시도해주세요.")
    }

    companion object {
        private const val PAIRING_PREFIX = "pairing:"
        private const val PAIRING_CODE_LENGTH = 6
        private const val PAIRING_TTL_MINUTES = 10L
        private const val MAX_CODE_GENERATION_ATTEMPTS = 5
        private const val MAX_SERIAL_GENERATION_ATTEMPTS = 5
        private const val MAX_SERIAL_BATCH_COUNT = 200
    }
}
