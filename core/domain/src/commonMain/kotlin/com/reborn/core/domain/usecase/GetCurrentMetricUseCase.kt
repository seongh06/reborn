package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.Metric

class GetCurrentMetricUseCase(
    private val metricRepository: MetricRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Metric> = metricRepository.getCurrent(deviceId)
}
