package com.reborn.core.data.datasource

import com.reborn.core.datastore.TokenLocalDataSource

class DeviceLocalDataSourceImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
) : DeviceLocalDataSource {

    override suspend fun saveDeviceCredentials(deviceId: String, appToken: String) {
        tokenLocalDataSource.saveDeviceCredentials(deviceId, appToken)
    }

    override suspend fun getDeviceId(): String? = tokenLocalDataSource.getDeviceId()

    override suspend fun getAppToken(): String? = tokenLocalDataSource.getAppToken()

    override suspend fun isAerometerDevice(): Boolean = tokenLocalDataSource.isAerometerDevice()
}
