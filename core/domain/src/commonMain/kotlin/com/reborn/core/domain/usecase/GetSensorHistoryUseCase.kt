package com.reborn.core.domain.usecase

import com.reborn.core.domain.UseCase
import com.reborn.core.domain.repository.SensorHistoryRepository
import com.reborn.core.model.SensorPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GetSensorHistoryParams(val deviceId: Int, val sensorType: String)

class GetSensorHistoryUseCase(
    private val repository: SensorHistoryRepository
) : UseCase<GetSensorHistoryParams, List<SensorPoint>> {
    override fun invoke(params: GetSensorHistoryParams): Flow<List<SensorPoint>> = flow {
        emit(repository.getSensorHistory(params.deviceId, params.sensorType))
    }
}
