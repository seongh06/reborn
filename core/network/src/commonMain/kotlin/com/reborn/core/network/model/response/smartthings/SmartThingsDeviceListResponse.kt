package com.reborn.core.network.model.response.smartthings

import kotlinx.serialization.Serializable

@Serializable
data class SmartThingsDeviceSummary(
    val deviceId: String,
    val label: String? = null,
)

@Serializable
data class SmartThingsDeviceListResponse(
    val devices: List<SmartThingsDeviceSummary>,
)
