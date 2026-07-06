package com.reborn.core.domain.repository

import com.reborn.core.model.SensorPoint

interface SensorHistoryRepository {
    suspend fun getSensorHistory(deviceId: Int, sensorType: String): List<SensorPoint>
}
