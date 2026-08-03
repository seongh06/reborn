package com.reborn.server.domain.device.dto

import com.reborn.server.domain.device.OperationMode
import com.reborn.server.domain.device.WindSpeed
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

class DeviceDto {

    data class RegisterRequest(
        @field:NotNull val placeId: Long? = null,
        @field:NotBlank val deviceId: String? = null,
        @field:NotBlank val deviceName: String? = null,
        // ARDUINO에 IR 송신 모듈이 실제로 물려있는 경우에만 true로 등록(#288) - 다른 기기 타입은 무시됨.
        val hasIrControl: Boolean = false,
    )

    data class RegisterResponse(
        val deviceId: String,
        val deviceName: String?,
        val deviceType: String,
        val category: String?,
        val createdAt: LocalDateTime,
    )

    data class SerialBatchRequest(
        @field:NotBlank val deviceType: String? = null,
        @field:NotNull val count: Int? = null,
    )

    data class SerialBatchResponse(
        val serials: List<String>,
    )

    data class GenerateAndRegisterRequest(
        @field:NotBlank val deviceType: String? = null,
        @field:NotNull val placeId: Long? = null,
        val deviceName: String? = null,
    )

    data class PairingCodeResponse(
        val pairingCode: String,
        val expiresAt: LocalDateTime,
    )

    data class PairingRequest(
        @field:NotBlank val pairingCode: String? = null,
        @field:NotBlank val deviceName: String? = null,
    )

    data class PairingResponse(
        val deviceId: String,
        val placeId: Long,
        val appToken: String,
    )

    data class ListResponse(
        val devices: List<DeviceItem>,
    )

    data class DeviceItem(
        val deviceId: String,
        val deviceName: String?,
        val deviceType: String,
        val category: String?,
        val isOnline: Boolean,
        val hasIrControl: Boolean,
        val createdAt: LocalDateTime,
    )

    // 전부 optional — 보낸 필드만 SmartThings 커맨드로 매핑되어 전송된다(#132).
    data class ControlRequest(
        val isPowerOn: Boolean? = null,
        val operationMode: OperationMode? = null,
        val windSpeed: WindSpeed? = null,
        val temperature: Int? = null,
    )

    data class ControlResponse(
        val deviceId: String,
        val sentAt: LocalDateTime,
    )

    // 아두이노가 폴링(GET /api/device/ir-command)해서 대기 중인 IR 명령을 가져갈 때 응답(#288).
    // command가 null이면 대기 중인 명령 없음 - 펌웨어는 이 경우 아무것도 안 하면 됨.
    data class IrCommandResponse(
        val command: String?,
    )

    // 필드가 null이면 이 기기가 그 capability를 지원하지 않는다는 뜻(#221) - 클라이언트가 원격 제어
    // 패널에서 해당 컨트롤 자체를 숨기는 기준으로 쓴다. ARDUINO/AI_SPEAKER는 이 개념이 없어 호출 대상 아님.
    data class StatusResponse(
        val isPowerOn: Boolean?,
        val operationMode: OperationMode?,
        val windSpeed: WindSpeed?,
        val temperature: Double?,
    )

    // 전부 자유 텍스트 - 클라이언트 UI가 숫자 입력이 아니라 프리셋 선택형이라 그대로 저장하고
    // 조건 평가 시점에만 숫자를 파싱한다(#190). 온도 상/하한만 실제로 실행됨.
    data class AutoControlRuleRequest(
        val discomfortThreshold: String? = null,
        val discomfortAction: String? = null,
        val humidityHighThreshold: String? = null,
        val humidityHighAction: String? = null,
        val humidityLowThreshold: String? = null,
        val humidityLowAction: String? = null,
        val temperatureHighThreshold: String? = null,
        val temperatureHighAction: String? = null,
        val temperatureLowThreshold: String? = null,
        val temperatureLowAction: String? = null,
        val occupancyThreshold: String? = null,
        val occupancyAction: String? = null,
        val isAutoOffEnabled: Boolean = false,
        val autoOffMinutes: String? = null,
    )

    // 시간 기반 자동제어 규칙(#325) - SmartThings 기기만 대상. daysOfWeek는 java.time.DayOfWeek
    // 이름("MONDAY" 등) 문자열 목록.
    data class ScheduleRuleRequest(
        @field:NotNull val hour: Int? = null,
        @field:NotNull val minute: Int? = null,
        val daysOfWeek: List<String>? = null,
        @field:NotNull val isPowerOn: Boolean? = null,
        val operationMode: OperationMode? = null,
    )

    data class ScheduleRuleResponse(
        val id: Long,
        val deviceId: String,
        val hour: Int,
        val minute: Int,
        val daysOfWeek: List<String>,
        val isPowerOn: Boolean,
        val operationMode: OperationMode?,
        val enabled: Boolean,
    )

    data class ScheduleRuleListResponse(
        val rules: List<ScheduleRuleResponse>,
    )

    data class ScheduleRuleUpdateRequest(
        @field:NotNull val enabled: Boolean? = null,
    )

    data class AutoControlRuleResponse(
        val discomfortThreshold: String?,
        val discomfortAction: String?,
        val humidityHighThreshold: String?,
        val humidityHighAction: String?,
        val humidityLowThreshold: String?,
        val humidityLowAction: String?,
        val temperatureHighThreshold: String?,
        val temperatureHighAction: String?,
        val temperatureLowThreshold: String?,
        val temperatureLowAction: String?,
        val occupancyThreshold: String?,
        val occupancyAction: String?,
        val isAutoOffEnabled: Boolean,
        val autoOffMinutes: String?,
    )
}
