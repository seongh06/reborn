package com.reborn.core.network.model.request.device

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val placeId: Long,
    val deviceId: String,
    val deviceName: String,
)
