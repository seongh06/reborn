package com.reborn.server.domain.device.dto

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
}
