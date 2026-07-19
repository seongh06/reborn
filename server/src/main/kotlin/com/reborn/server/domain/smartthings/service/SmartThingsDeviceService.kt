package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.device.Device
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

        val device = try {
            deviceRepository.save(
                Device(
                    place = place,
                    deviceType = DeviceType.SMART_THINGS,
                    deviceKey = deviceKey,
                    name = deviceName,
                    isOnline = true,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        return DeviceConverter.toRegisterResponse(device)
    }

    @Transactional
    fun control(userId: Long, deviceKey: String, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        if (device.deviceType != DeviceType.SMART_THINGS) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "SmartThings로 제어할 수 없는 기기입니다.")
        }
        requireAdmin(userId, device.place.id)

        val commands = buildCommands(request)
        if (commands.isEmpty()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "제어할 항목이 없습니다.")
        }

        val accessToken = smartThingsService.getValidAccessToken(device.place.id)
        smartThingsDeviceClient.sendCommands(accessToken, deviceKey, commands)

        return DeviceDto.ControlResponse(deviceId = deviceKey, sentAt = LocalDateTime.now())
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

    private fun requireAdmin(userId: Long, placeId: Long) {
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }
}
