package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toSensorPoints
import com.reborn.core.domain.repository.SensorHistoryRepository
import com.reborn.core.model.SensorPoint
import com.reborn.core.network.service.SensorHistoryApi

class SensorHistoryRepositoryImpl(
    private val api: SensorHistoryApi
) : SensorHistoryRepository {
    override suspend fun getSensorHistory(deviceId: Int, sensorType: String): List<SensorPoint> =
        api.getSensorHistory(deviceId, sensorType).toSensorPoints()
}
