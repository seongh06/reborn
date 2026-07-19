package com.reborn.core.data.repository

import com.reborn.core.data.datasource.DeviceLocalDataSource
import com.reborn.core.data.mapper.toDevice
import com.reborn.core.data.mapper.toPairedDevice
import com.reborn.core.data.mapper.toPairingCode
import com.reborn.core.data.mapper.toRegisteredDevice
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.Device
import com.reborn.core.model.PairedDevice
import com.reborn.core.model.PairingCode
import com.reborn.core.model.RegisteredDevice
import com.reborn.core.network.datasource.DeviceDataSource
import com.reborn.core.network.model.request.device.PairingRequest
import com.reborn.core.network.model.request.device.RegisterDeviceRequest

class DeviceRepositoryImpl(
    private val remote: DeviceDataSource,
    private val deviceLocal: DeviceLocalDataSource,
) : DeviceRepository {

    override suspend fun generatePairingCode(placeId: Long): Result<PairingCode> =
        remote.generatePairingCode(placeId)
            .toResult { it.toPairingCode() }

    override suspend fun pairDevice(pairingCode: String, deviceName: String): Result<PairedDevice> =
        remote.pairDevice(PairingRequest(pairingCode, deviceName))
            .toResult { it.toPairedDevice() }
            .onSuccess { deviceLocal.saveDeviceCredentials(it.deviceId, it.appToken) }

    override suspend fun getList(placeId: Long): Result<List<Device>> =
        remote.getList(placeId)
            .toResult { response -> response.devices.map { it.toDevice() } }

    override suspend fun registerDevice(placeId: Long, deviceId: String, deviceName: String): Result<RegisteredDevice> =
        remote.registerDevice(RegisterDeviceRequest(placeId, deviceId, deviceName))
            .toResult { it.toRegisteredDevice() }
}
