package com.reborn.core.domain.usecase

import com.reborn.core.domain.UseCase
import com.reborn.core.domain.repository.SensorHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GetAnalysisTextParams(val deviceId: String, val sensorType: String)

class GetAnalysisTextUseCase(
    private val repository: SensorHistoryRepository
) : UseCase<GetAnalysisTextParams, String> {
    override fun invoke(params: GetAnalysisTextParams): Flow<String> = flow {
        emit(repository.getAnalysisText(params.deviceId, params.sensorType))
    }
}
