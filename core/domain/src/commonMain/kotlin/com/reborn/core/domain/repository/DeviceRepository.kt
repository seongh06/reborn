package com.reborn.core.domain.repository

import com.reborn.core.model.AutoControlRule
import com.reborn.core.model.Device
import com.reborn.core.model.DeviceStatus
import com.reborn.core.model.PairedDevice
import com.reborn.core.model.PairingCode
import com.reborn.core.model.RegisteredDevice

interface DeviceRepository {
    suspend fun generatePairingCode(placeId: Long): Result<PairingCode>

    suspend fun pairDevice(pairingCode: String, deviceName: String): Result<PairedDevice>

    suspend fun getList(placeId: Long): Result<List<Device>>

    suspend fun registerDevice(placeId: Long, deviceId: String, deviceName: String): Result<RegisteredDevice>

    suspend fun getStatus(deviceId: String): Result<DeviceStatus>

    suspend fun controlDevice(
        deviceId: String,
        isPowerOn: Boolean?,
        operationMode: String?,
        windSpeed: String?,
        temperature: Int?,
    ): Result<Unit>

    suspend fun deleteDevice(deviceId: String): Result<Unit>

    suspend fun saveAutoControlRule(deviceId: String, rule: AutoControlRule): Result<AutoControlRule>

    suspend fun getAutoControlRule(deviceId: String): Result<AutoControlRule?>

    // 페어링 성공 시 로컬(DataStore)에 저장해둔 이 인스턴스 자신의 deviceId(#294) - 공기계 앱이
    // 자기 자신을 식별해 메트릭을 전송할 때 필요. 페어링 전이면 null.
    suspend fun getLocalDeviceId(): String?
}
