package com.reborn.core.domain.repository

import com.reborn.core.model.RegisteredDevice
import com.reborn.core.model.SmartThingsDevice

interface SmartThingsRepository {
    suspend fun getAuthorizeUrl(placeId: Long): Result<String>
    suspend fun getDevices(placeId: Long): Result<List<SmartThingsDevice>>
    suspend fun registerDevice(placeId: Long, smartThingsDeviceId: String, deviceName: String): Result<RegisteredDevice>
}
