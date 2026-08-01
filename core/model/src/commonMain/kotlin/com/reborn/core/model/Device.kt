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
    // 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/AIR_PURIFIER/VENTILATOR/OTHER) - SmartThings만 값이 있을 수 있고,
    // Arduino/AI스피커/공기계는 항상 null(UI에서 OTHER로 처리)
    val category: String? = null,
)

// 필드가 null이면 이 기기가 그 컨트롤을 지원하지 않는다는 뜻(#221) - SMART_THINGS 기기에만 의미 있음.
data class DeviceStatus(
    val isPowerOn: Boolean?,
    val operationMode: String?,
    val windSpeed: String?,
    val temperature: Double?,
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
