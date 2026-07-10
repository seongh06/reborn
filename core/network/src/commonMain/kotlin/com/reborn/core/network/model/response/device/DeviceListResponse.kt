package com.reborn.core.network.model.response.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceListResponse(
    val devices: List<DeviceItemResponse>,
)

@Serializable
data class DeviceItemResponse(
    val deviceId: String,
    val deviceName: String?,
    val deviceType: String,
    val isOnline: Boolean,
    val createdAt: String,
)
