package com.reborn.core.network.model.request.smartthings

import kotlinx.serialization.Serializable

@Serializable
data class RegisterSmartThingsDeviceRequest(
    val placeId: Long,
    val smartThingsDeviceId: String,
    val deviceName: String,
    // 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER) - core:ui DeviceType과 이름을 맞춤
    val category: String? = null,
)
