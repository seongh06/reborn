package com.reborn.core.model

data class PairingCode(
    val code: String,
    val expiresAt: String,
)

data class PairedDevice(
    val deviceId: String,
    val placeId: Long,
    val appToken: String,
)

data class Device(
    val deviceId: String,
    val deviceName: String?,
    val deviceType: String,
    val isOnline: Boolean,
    val createdAt: String,
    // 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER) - SmartThings만 값이 있을 수 있고,
    // Arduino/AI스피커/공기계는 항상 null(UI에서 OTHER로 처리)
    val category: String? = null,
)

data class RegisteredDevice(
    val deviceId: String,
    val deviceName: String?,
    val deviceType: String,
    val createdAt: String,
    val category: String? = null,
)

data class AutoControlRule(
    val discomfortThreshold: String?,
    val discomfortAction: String?,
    val humidityHighThreshold: String?,
    val humidityHighAction: String?,
    val humidityLowThreshold: String?,
    val humidityLowAction: String?,
    val temperatureHighThreshold: String?,
    val temperatureHighAction: String?,
    val temperatureLowThreshold: String?,
    val temperatureLowAction: String?,
    val occupancyThreshold: String?,
    val occupancyAction: String?,
    val isAutoOffEnabled: Boolean,
    val autoOffMinutes: String?,
)
