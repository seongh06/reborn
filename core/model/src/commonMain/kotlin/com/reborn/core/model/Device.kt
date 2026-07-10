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
)
