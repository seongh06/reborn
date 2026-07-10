package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.MetricSnapshot

class GetCurrentMetricUseCase(
    private val metricRepository: MetricRepository
) {
    suspend operator fun invoke(deviceId: String): Result<MetricSnapshot> {
        return metricRepository.getCurrent(deviceId)
    }
}
