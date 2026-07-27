package com.reborn.core.network.model.request.smartthings

import kotlinx.serialization.Serializable

@Serializable
data class RegisterSmartThingsDeviceRequest(
    val placeId: Long,
    val smartThingsDeviceId: String,
    val deviceName: String,
)
