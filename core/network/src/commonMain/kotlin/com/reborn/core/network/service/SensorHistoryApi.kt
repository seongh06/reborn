package com.reborn.core.network.service

import com.reborn.core.network.model.SensorHistoryResponse

interface SensorHistoryApi {
    suspend fun getSensorHistory(deviceId: String, sensorType: String): SensorHistoryResponse
}
