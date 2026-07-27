package com.reborn.core.data.repository

import com.reborn.core.domain.repository.DeviceProvisioningRepository
import com.reborn.core.network.datasource.DeviceProvisioningDataSource

class DeviceProvisioningRepositoryImpl(
    private val remote: DeviceProvisioningDataSource,
) : DeviceProvisioningRepository {

    override suspend fun configureWifi(ssid: String, password: String, deviceId: String): Result<Unit> =
        remote.configureWifi(ssid, password, deviceId)
}
