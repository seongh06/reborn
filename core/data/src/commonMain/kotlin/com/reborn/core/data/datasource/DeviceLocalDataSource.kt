package com.reborn.core.data.datasource

interface DeviceLocalDataSource {
    suspend fun saveDeviceCredentials(deviceId: String, appToken: String)
    suspend fun getDeviceId(): String?
    suspend fun getAppToken(): String?
    suspend fun isAerometerDevice(): Boolean
}
