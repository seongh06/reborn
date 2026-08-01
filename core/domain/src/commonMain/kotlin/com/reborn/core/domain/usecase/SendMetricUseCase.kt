package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.MetricRepository

class SendMetricUseCase(
    private val metricRepository: MetricRepository
) {
    suspend operator fun invoke(deviceId: String, illuminance: Int, peopleCount: Int): Result<Unit> =
        metricRepository.collect(deviceId, illuminance, peopleCount)
}
