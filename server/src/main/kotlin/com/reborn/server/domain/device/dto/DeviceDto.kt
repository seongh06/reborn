package com.reborn.server.domain.device.dto

import java.time.LocalDateTime

class DeviceDto {

    data class RegisterRequest(
        val placeId: Long? = null,
        val deviceId: String? = null,
        val deviceName: String? = null,
    )

    data class RegisterResponse(
        val deviceId: String,
        val deviceName: String?,
        val deviceType: String,
        val createdAt: LocalDateTime,
    )
}
