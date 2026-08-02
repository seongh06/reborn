package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.AutoControlRule
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceSerial
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.converter.DeviceConverter
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.AutoControlRuleRepository
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.device.repository.DeviceSerialRepository
import com.reborn.server.domain.metric.MetricLogRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.util.generateDeviceSerial
import com.reborn.server.global.util.generateRandomCode
import com.reborn.server.global.util.generateUuid
import com.reborn.server.domain.smartthings.service.SmartThingsDeviceService
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
    private val autoControlRuleRepository: AutoControlRuleRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val redisUtil: RedisUtil,
    private val smartThingsDeviceService: SmartThingsDeviceService,
    private val arduinoIrControlService: ArduinoIrControlService,
    private val metricLogRepository: MetricLogRepository,
    @param:Value("\${operator.api-key:}") private val operatorApiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ARDUINO/AEROMETER는 markOnline()의 "연결된 적 있음" 1회성 플래그(#276) 대신 최근 metric_logs
    // 수신 시각 기준으로 온라인 여부를 판단한다(#307) - WiFi가 끊긴 뒤에도 계속 온라인으로 보이던 문제 수정.
    // AI_SPEAKER/SMART_THINGS는 주기적으로 메트릭을 보내는 기기가 아니라 기존 markOnline() 플래그를 그대로 쓴다.
    private val recencyBasedOnlineTypes = setOf(DeviceType.ARDUINO, DeviceType.AEROMETER)
    private val onlineMetricWindow: Duration = Duration.ofMinutes(10)

    private fun resolveIsOnline(device: Device): Boolean {
        if (device.deviceType !in recencyBasedOnlineTypes) return device.isOnline
        val lastMetricAt = metricLogRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)?.createdAt ?: return false
        return Duration.between(lastMetricAt, LocalDateTime.now()) <= onlineMetricWindow
    }

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

        // hasIrControl은 ARDUINO가 아닌 타입이 잘못 보내도 조용히 무시 - device_serial이 이미 실제
        // 타입을 결정하므로, 여기서 에러를 내는 것보다 타입에 안 맞는 요청값을 버리는 쪽이 자연스럽다.
        val hasIrControl = request.hasIrControl && deviceSerial.deviceType == DeviceType.ARDUINO

        val device = try {
            deviceRepository.save(
                Device(
                    place = place,
                    deviceType = deviceSerial.deviceType,
                    deviceKey = serial,
                    name = deviceName,
                    hasIrControl = hasIrControl,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("register 기기 저장 중 무결성 위반: {}", e.message)
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }
        deviceSerial.assignTo(device)
        deviceSerialRepository.save(deviceSerial)

        return DeviceConverter.toRegisterResponse(device)
    }

    // SmartThings/아두이노(IR) 제어 요청 단일 진입점(#288) - 기기 타입에 따라 알맞은 서비스로 위임.
    // 각 서비스가 자체적으로 타입 검증 + ADMIN 권한 확인을 다시 하므로 여기서 중복 검사하지 않는다.
    @Transactional
    fun control(userId: Long, deviceKey: String, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")

        return when {
            device.deviceType == DeviceType.SMART_THINGS ->
                smartThingsDeviceService.control(userId, deviceKey, request)
            device.deviceType == DeviceType.ARDUINO && device.hasIrControl ->
                arduinoIrControlService.control(userId, deviceKey, request)
            else -> throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "제어할 수 없는 기기입니다.")
        }
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

        val serials = (1..count).map { reserveUniqueSerial(prefix, deviceType).serial }
        return DeviceDto.SerialBatchResponse(serials = serials)
    }

    // #150: place가 이미 만들어져 있는 상태에서 운영자가 하드웨어를 준비/배송할 때, 시리얼 발급과
    // 그 장소로의 기기 등록을 한 번에 끝낸다 — 이후 실물에 이 시리얼만 입력해서 실행하면 되고,
    // 고객사 관리자가 앱에서 별도로 등록할 필요가 없다. generateSerialBatch(재고용, 장소 무관)와는
    // 별개 경로 — 여기서는 device_serial 생성과 동시에 assignTo까지 끝낸다.
    @Transactional
    fun generateAndRegisterDevice(
        operatorKey: String,
        deviceType: DeviceType,
        placeId: Long,
        deviceName: String?,
    ): DeviceDto.RegisterResponse {
        requireOperator(operatorKey)

        val prefix = deviceType.serialPrefix
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "시리얼 발급 대상이 아닌 기기 유형입니다.")
        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        val deviceSerial = reserveUniqueSerial(prefix, deviceType)
        val device = try {
            deviceRepository.save(
                Device(
                    place = place,
                    deviceType = deviceType,
                    deviceKey = deviceSerial.serial,
                    name = deviceName?.takeIf { it.isNotBlank() },
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("generateAndRegisterDevice 기기 저장 중 무결성 위반: {}", e.message)
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }
        deviceSerial.assignTo(device)
        deviceSerialRepository.save(deviceSerial)

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

        val devices = deviceRepository.findAllByPlaceId(placeId)
            .map { DeviceConverter.toDeviceItem(it, isOnline = resolveIsOnline(it)) }
        return DeviceDto.ListResponse(devices = devices)
    }

    // 기기(아두이노/AI 스피커)가 실제로 WiFi 연결에 성공한 직후 스스로 호출하는 하트비트(#276).
    // 인증 없이 deviceKey만으로 신뢰한다 - X-Device-Id 기반 다른 하드웨어 엔드포인트와 동일한
    // 인증 모델(DeviceType.kt 주석 참고). 등록만 되고 실제로 한 번도 연결에 성공한 적 없는
    // 기기를 앱이 구분해서 보여줄 수 있게 하는 게 목적 - isOnline은 한 번 true가 되면 다시
    // false로 돌아가지 않으므로("연결된 적 있음" 마커로 재사용) 기기가 재부팅될 때마다 계속
    // 호출해도 무해하다.
    @Transactional
    fun markOnline(deviceKey: String) {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        device.updateOnlineStatus(true)
    }

    @Transactional
    fun delete(userId: Long, deviceKey: String) {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        requireAdmin(userId, device.place.id)
        deviceRepository.delete(device)
    }

    @Transactional
    fun saveAutoControlRule(
        userId: Long,
        deviceKey: String,
        request: DeviceDto.AutoControlRuleRequest,
    ): DeviceDto.AutoControlRuleResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        requireAdmin(userId, device.place.id)

        val rule = autoControlRuleRepository.findByDeviceId(device.id)
            ?: AutoControlRule(device = device)
        rule.apply {
            discomfortThreshold = request.discomfortThreshold
            discomfortAction = request.discomfortAction
            humidityHighThreshold = request.humidityHighThreshold
            humidityHighAction = request.humidityHighAction
            humidityLowThreshold = request.humidityLowThreshold
            humidityLowAction = request.humidityLowAction
            temperatureHighThreshold = request.temperatureHighThreshold
            temperatureHighAction = request.temperatureHighAction
            temperatureLowThreshold = request.temperatureLowThreshold
            temperatureLowAction = request.temperatureLowAction
            occupancyThreshold = request.occupancyThreshold
            occupancyAction = request.occupancyAction
            isAutoOffEnabled = request.isAutoOffEnabled
            autoOffMinutes = request.autoOffMinutes
        }
        val saved = autoControlRuleRepository.save(rule)
        return DeviceConverter.toAutoControlRuleResponse(saved)
    }

    fun getAutoControlRule(userId: Long, deviceKey: String): DeviceDto.AutoControlRuleResponse? {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        requireAdmin(userId, device.place.id)

        return autoControlRuleRepository.findByDeviceId(device.id)
            ?.let { DeviceConverter.toAutoControlRuleResponse(it) }
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

    private fun reserveUniqueSerial(prefix: String, deviceType: DeviceType): DeviceSerial {
        repeat(MAX_SERIAL_GENERATION_ATTEMPTS) {
            val serial = generateDeviceSerial(prefix)
            try {
                return deviceSerialRepository.save(DeviceSerial(serial = serial, deviceType = deviceType))
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
