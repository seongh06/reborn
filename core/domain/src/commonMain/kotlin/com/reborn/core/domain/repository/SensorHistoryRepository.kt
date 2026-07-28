package com.reborn.core.domain.repository

import com.reborn.core.model.SensorPoint

interface SensorHistoryRepository {
    suspend fun getSensorHistory(deviceId: String, sensorType: String): List<SensorPoint>

    suspend fun getSensorAggregate(deviceId: String, sensorType: String, period: String): List<Float>

    suspend fun getAnalysisText(deviceId: String, sensorType: String): String

    suspend fun exportToSheets(deviceId: String): String
}
