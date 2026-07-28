package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toSensorPoints
import com.reborn.core.domain.repository.SensorHistoryRepository
import com.reborn.core.model.SensorPoint
import com.reborn.core.network.service.SensorHistoryApi

class SensorHistoryRepositoryImpl(
    private val api: SensorHistoryApi
) : SensorHistoryRepository {
    override suspend fun getSensorHistory(deviceId: String, sensorType: String): List<SensorPoint> =
        api.getSensorHistory(deviceId, sensorType).toSensorPoints()

    // 값이 없는 버킷(0으로 나눠지는 구간 등)은 HOUR 집계와 동일하게 0.0으로 채운다
    override suspend fun getSensorAggregate(deviceId: String, sensorType: String, period: String): List<Float> =
        api.getSensorAggregate(deviceId, sensorType, period).map { (it ?: 0.0).toFloat() }

    override suspend fun getAnalysisText(deviceId: String, sensorType: String): String =
        api.getAnalysisText(deviceId, sensorType)

    override suspend fun exportToSheets(deviceId: String): String =
        api.exportToSheets(deviceId)
}
