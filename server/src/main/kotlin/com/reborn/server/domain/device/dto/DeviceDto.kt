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
    )

    data class RegisterResponse(
        val deviceId: String,
        val deviceName: String?,
        val deviceType: String,
        val createdAt: LocalDateTime,
    )

    data class SerialBatchRequest(
        @field:NotBlank val deviceType: String? = null,
        @field:NotNull val count: Int? = null,
    )

    data class SerialBatchResponse(
        val serials: List<String>,
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
        val isOnline: Boolean,
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
}
