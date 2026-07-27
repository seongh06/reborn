package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class ControlDeviceResponse(
    val deviceId: String,
    val sentAt: String,
)
