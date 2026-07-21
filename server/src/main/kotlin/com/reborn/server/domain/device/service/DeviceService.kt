package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.converter.DeviceConverter
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.util.generateRandomCode
import com.reborn.server.global.util.generateUuid
import org.slf4j.LoggerFactory
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
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val redisUtil: RedisUtil,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(userId: Long, request: DeviceDto.RegisterRequest): DeviceDto.RegisterResponse {
        val placeId = request.placeId
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "장소 ID는 필수입니다.")
        val deviceKey = request.deviceId?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 ID는 필수입니다.")
        val deviceName = request.deviceName?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 이름은 필수입니다.")
        val deviceType = request.deviceType ?: DeviceType.ARDUINO
        if (deviceType != DeviceType.ARDUINO && deviceType != DeviceType.AI_SPEAKER) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이 엔드포인트로는 ARDUINO/AI_SPEAKER 기기만 등록할 수 있습니다.")
        }

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        requireAdmin(userId, placeId)

        if (deviceRepository.existsByDeviceKey(deviceKey)) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        val device = try {
            deviceRepository.save(
                Device(place = place, deviceType = deviceType, deviceKey = deviceKey, name = deviceName),
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("register 기기 저장 중 무결성 위반: {}", e.message)
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        return DeviceConverter.toRegisterResponse(device)
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
    }
}
