package com.reborn.core.domain.usecase

import com.reborn.core.domain.UseCase
import com.reborn.core.domain.repository.SensorHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GetSensorAggregateParams(val deviceId: String, val sensorType: String, val period: String)

class GetSensorAggregateUseCase(
    private val repository: SensorHistoryRepository
) : UseCase<GetSensorAggregateParams, List<Float>> {
    override fun invoke(params: GetSensorAggregateParams): Flow<List<Float>> = flow {
        emit(repository.getSensorAggregate(params.deviceId, params.sensorType, params.period))
    }
}
