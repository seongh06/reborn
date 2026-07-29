package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceCategory
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.OperationMode
import com.reborn.server.domain.device.WindSpeed
import com.reborn.server.domain.device.converter.DeviceConverter
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.client.SmartThingsCommand
import com.reborn.server.domain.smartthings.client.SmartThingsDeviceClient
import com.reborn.server.domain.smartthings.dto.SmartThingsDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// SmartThings 기기 조회·등록·제어(#132). OAuth 토큰 발급/리프레시는 SmartThingsService(#130)에 위임한다.
@Service
@Transactional(readOnly = true)
class SmartThingsDeviceService(
    private val smartThingsService: SmartThingsService,
    private val smartThingsDeviceClient: SmartThingsDeviceClient,
    private val placeRepository: PlaceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val deviceRepository: DeviceRepository,
) {

    fun listDevices(userId: Long, placeId: Long): SmartThingsDto.DeviceListResponse {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        requireAdmin(userId, placeId)

        val accessToken = smartThingsService.getValidAccessToken(placeId)
        val devices = smartThingsDeviceClient.getDevices(accessToken)
            .map { SmartThingsDto.DeviceSummary(deviceId = it.deviceId, label = it.label) }

        return SmartThingsDto.DeviceListResponse(devices = devices)
    }

    @Transactional
    fun registerDevice(userId: Long, request: SmartThingsDto.RegisterDeviceRequest): DeviceDto.RegisterResponse {
        val placeId = request.placeId
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "장소 ID는 필수입니다.")
        val deviceKey = request.smartThingsDeviceId?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "SmartThings 기기 ID는 필수입니다.")
        val deviceName = request.deviceName?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 이름은 필수입니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        requireAdmin(userId, placeId)
        val category = parseCategory(request.category)

        val device = try {
            deviceRepository.save(
                Device(
                    place = place,
                    deviceType = DeviceType.SMART_THINGS,
                    deviceKey = deviceKey,
                    name = deviceName,
                    category = category,
                    isOnline = true,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        return DeviceConverter.toRegisterResponse(device)
    }

    private fun parseCategory(category: String?): String? {
        if (category.isNullOrBlank()) return null
        return runCatching { DeviceCategory.valueOf(category) }
            .getOrElse { throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "잘못된 기기 카테고리입니다.") }
            .name
    }

    // 기기 상세 화면 진입 시 현재 전원/운전모드/바람세기/희망온도를 실제로 조회해 초기값으로
    // 반영하고, 지원하지 않는 컨트롤은 화면에서 숨길 수 있게 한다(#221).
    fun getStatus(userId: Long, deviceKey: String): DeviceDto.StatusResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        if (device.deviceType != DeviceType.SMART_THINGS) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "SmartThings 기기가 아닙니다.")
        }
        requireAdmin(userId, device.place.id)

        val accessToken = smartThingsService.getValidAccessToken(device.place.id)
        val status = smartThingsDeviceClient.getDeviceStatus(accessToken, device.deviceKey)

        return DeviceDto.StatusResponse(
            isPowerOn = status.isPowerOn,
            operationMode = smartThingsToOperationMode(status.operationMode),
            windSpeed = smartThingsToWindSpeed(status.windSpeed),
            temperature = status.targetTemperature,
        )
    }

    @Transactional
    fun control(userId: Long, deviceKey: String, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        if (device.deviceType != DeviceType.SMART_THINGS) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "SmartThings로 제어할 수 없는 기기입니다.")
        }
        requireAdmin(userId, device.place.id)
        return controlInternal(device, request)
    }

    // 사용자 요청(control)과 자동 제어 스케줄러(AutoControlEvaluationService) 양쪽에서 재사용 -
    // 스케줄러는 userId(요청자)가 없어 ADMIN 권한 검증을 건너뛰어야 하므로 그 부분만 상위에서 분리.
    fun controlInternal(device: Device, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        if (device.deviceType != DeviceType.SMART_THINGS) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "SmartThings로 제어할 수 없는 기기입니다.")
        }

        val commands = buildCommands(request)
        if (commands.isEmpty()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "제어할 항목이 없습니다.")
        }

        val accessToken = smartThingsService.getValidAccessToken(device.place.id)
        smartThingsDeviceClient.sendCommands(accessToken, device.deviceKey, commands)

        return DeviceDto.ControlResponse(deviceId = device.deviceKey, sentAt = LocalDateTime.now())
    }

    // 전원/운전모드/온도/풍량 → SmartThings capability 커맨드 매핑(#132 이슈에 정의된 매핑표 그대로).
    private fun buildCommands(request: DeviceDto.ControlRequest): List<SmartThingsCommand> {
        val commands = mutableListOf<SmartThingsCommand>()

        request.isPowerOn?.let { on ->
            commands += SmartThingsCommand(capability = "switch", command = if (on) "on" else "off")
        }
        request.operationMode?.let { mode ->
            commands += SmartThingsCommand(
                capability = "airConditionerMode",
                command = "setAirConditionerMode",
                arguments = listOf(operationModeToSmartThings(mode)),
            )
        }
        request.windSpeed?.let { speed ->
            commands += SmartThingsCommand(
                capability = "airConditionerFanMode",
                command = "setFanMode",
                arguments = listOf(windSpeedToSmartThings(speed)),
            )
        }
        request.temperature?.let { temp ->
            commands += SmartThingsCommand(
                capability = "thermostatCoolingSetpoint",
                command = "setCoolingSetpoint",
                arguments = listOf(temp),
            )
        }

        return commands
    }

    private fun operationModeToSmartThings(mode: OperationMode): String = when (mode) {
        OperationMode.COOL -> "cool"
        OperationMode.HEAT -> "heat"
        OperationMode.DEHUMIDIFY -> "dry"
        OperationMode.FAN -> "wind"
    }

    private fun windSpeedToSmartThings(speed: WindSpeed): String = when (speed) {
        WindSpeed.LOW -> "low"
        WindSpeed.MEDIUM -> "medium"
        WindSpeed.HIGH -> "high"
        WindSpeed.AUTO -> "auto"
    }

    // operationModeToSmartThings/windSpeedToSmartThings의 역방향 - SmartThings가 우리가 모르는 값을
    // 내려주거나(예: 기기 자체 확장 모드) capability 자체가 없으면(null) 그대로 null 반환, 예외 아님.
    private fun smartThingsToOperationMode(raw: String?): OperationMode? = when (raw) {
        "cool" -> OperationMode.COOL
        "heat" -> OperationMode.HEAT
        "dry" -> OperationMode.DEHUMIDIFY
        "wind" -> OperationMode.FAN
        else -> null
    }

    private fun smartThingsToWindSpeed(raw: String?): WindSpeed? = when (raw) {
        "low" -> WindSpeed.LOW
        "medium" -> WindSpeed.MEDIUM
        "high" -> WindSpeed.HIGH
        "auto" -> WindSpeed.AUTO
        else -> null
    }

    private fun requireAdmin(userId: Long, placeId: Long) {
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }
}
