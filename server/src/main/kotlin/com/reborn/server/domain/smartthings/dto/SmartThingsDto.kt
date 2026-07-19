package com.reborn.server.domain.smartthings.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class SmartThingsDto {

    data class AuthorizeResponse(
        val authorizeUrl: String,
    )

    data class DeviceSummary(
        val deviceId: String,
        val label: String?,
    )

    data class DeviceListResponse(
        val devices: List<DeviceSummary>,
    )

    data class RegisterDeviceRequest(
        @field:NotNull val placeId: Long? = null,
        @field:NotBlank val smartThingsDeviceId: String? = null,
        @field:NotBlank val deviceName: String? = null,
    )
}
