package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toRegisteredDevice
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.SmartThingsRepository
import com.reborn.core.model.RegisteredDevice
import com.reborn.core.model.SmartThingsDevice
import com.reborn.core.network.datasource.SmartThingsDataSource
import com.reborn.core.network.model.request.smartthings.RegisterSmartThingsDeviceRequest

class SmartThingsRepositoryImpl(
    private val remote: SmartThingsDataSource,
) : SmartThingsRepository {

    override suspend fun getAuthorizeUrl(placeId: Long): Result<String> =
        remote.getAuthorizeUrl(placeId)
            .toResult { it.authorizeUrl }

    override suspend fun getDevices(placeId: Long): Result<List<SmartThingsDevice>> =
        remote.getDevices(placeId)
            .toResult { response ->
                response.devices.map { SmartThingsDevice(deviceId = it.deviceId, label = it.label) }
            }

    override suspend fun registerDevice(
        placeId: Long,
        smartThingsDeviceId: String,
        deviceName: String,
    ): Result<RegisteredDevice> =
        remote.registerDevice(RegisterSmartThingsDeviceRequest(placeId, smartThingsDeviceId, deviceName))
            .toResult { it.toRegisteredDevice() }
}
