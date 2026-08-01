package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.IrCommand
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// 아두이노 IR 에어컨 제어(#288). SmartThingsDeviceService.control()/controlInternal()과 동일 패턴 -
// 실제 발사는 서버가 아니라 아두이노가 GET /api/device/ir-command 폴링으로 가져가서 수행한다(비동기,
// SmartThings처럼 즉시 반영을 보장하지 않음 - 최대 폴링 주기만큼 지연될 수 있음).
//
// ⚠️ 상태지정형(ON/OFF 별도 코드) 리모컨이라는 가정 하에 만든 MVP. 토글형(단일 코드)으로 밝혀지면
// AutoControlEvaluationService에서 이 서비스를 반복 호출하는 것 자체가 위험해진다(#288 이슈 참고) -
// 그 경우 온도 추이로 실제 효과를 확인한 뒤에만 재전송하는 안전장치를 추가로 넣어야 한다.
@Service
@Transactional(readOnly = true)
class ArduinoIrControlService(
    private val deviceRepository: DeviceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
) {

    @Transactional
    fun control(userId: Long, deviceKey: String, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")
        requireIrCapable(device)
        requireAdmin(userId, device.place.id)
        return controlInternal(device, request)
    }

    // 사용자 요청(control)과 자동 제어 스케줄러(AutoControlEvaluationService) 양쪽에서 재사용 -
    // 스케줄러는 userId(요청자)가 없어 ADMIN 권한 검증을 건너뛰어야 하므로 그 부분만 상위에서 분리.
    @Transactional
    fun controlInternal(device: Device, request: DeviceDto.ControlRequest): DeviceDto.ControlResponse {
        requireIrCapable(device)

        val command = buildIrCommand(request)
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "제어할 항목이 없습니다.")

        device.queuePendingIrCommand(command)
        return DeviceDto.ControlResponse(deviceId = device.deviceKey, sentAt = LocalDateTime.now())
    }

    // 아두이노가 폴링해서 대기 중인 명령을 1회성으로 가져간다 - 소비 즉시 비워서 중복 실행을 막는다.
    @Transactional
    fun consumePendingCommand(deviceKey: String): DeviceDto.IrCommandResponse {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 기기입니다.")

        val command = device.pendingIrCommand
        if (command != null) {
            device.clearPendingIrCommand()
        }
        return DeviceDto.IrCommandResponse(command = command?.name)
    }

    private fun requireIrCapable(device: Device) {
        if (device.deviceType != DeviceType.ARDUINO || !device.hasIrControl) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "IR로 제어할 수 없는 기기입니다.")
        }
    }

    // isPowerOn/temperature만 사용 - operationMode/windSpeed는 IR MVP 어휘(IrCommand)에 대응하는
    // 캡처된 신호가 없어 무시된다.
    private fun buildIrCommand(request: DeviceDto.ControlRequest): IrCommand? = when {
        request.isPowerOn == false -> IrCommand.POWER_OFF
        request.temperature != null -> mapTemperatureToCommand(request.temperature)
        request.isPowerOn == true -> IrCommand.POWER_ON
        else -> null
    }

    // MVP는 캡처된 온도 프리셋이 COOL_24 하나뿐이라 요청 온도와 무관하게 폴백 - 리모컨에서 추가
    // 온도를 캡처해 IrCommand에 값을 늘릴 때 이 매핑도 같이 확장할 것.
    private fun mapTemperatureToCommand(@Suppress("UNUSED_PARAMETER") temperature: Int): IrCommand = IrCommand.COOL_24

    private fun requireAdmin(userId: Long, placeId: Long) {
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }
}
