package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.MetricHistoryPage

data class GetMetricHistoryParams(val deviceId: String, val page: Int = 0, val size: Int = 200)

class GetMetricHistoryUseCase(
    private val metricRepository: MetricRepository
) {
    suspend operator fun invoke(params: GetMetricHistoryParams): Result<MetricHistoryPage> {
        return metricRepository.getHistory(params.deviceId, params.page, params.size)
    }
}
