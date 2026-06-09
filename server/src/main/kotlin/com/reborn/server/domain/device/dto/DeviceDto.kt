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
}
