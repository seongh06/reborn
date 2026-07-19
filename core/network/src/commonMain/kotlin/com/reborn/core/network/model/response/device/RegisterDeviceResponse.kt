package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceResponse(
    val deviceId: String,
    val deviceName: String?,
    val deviceType: String,
    val createdAt: String,
)
