package com.reborn.core.domain.usecase

import com.reborn.core.domain.UseCase
import com.reborn.core.domain.repository.SensorHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExportMetricToSheetsUseCase(
    private val repository: SensorHistoryRepository
) : UseCase<String, String> {
    override fun invoke(params: String): Flow<String> = flow {
        emit(repository.exportToSheets(params))
    }
}
