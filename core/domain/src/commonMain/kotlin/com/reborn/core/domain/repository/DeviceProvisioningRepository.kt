package com.reborn.core.domain.repository

interface DeviceProvisioningRepository {
    suspend fun configureWifi(ssid: String, password: String, deviceId: String): Result<Unit>
}
