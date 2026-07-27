package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.Metric
import com.reborn.core.network.datasource.MetricDataSource

class MetricRepositoryImpl(
    private val remote: MetricDataSource,
) : MetricRepository {

    override suspend fun getCurrent(deviceId: String): Result<Metric> =
        remote.getCurrent(deviceId)
            .toResult { response ->
                Metric(
                    temperature = response.temperature,
                    humidity = response.humidity,
                    illuminance = response.illuminance,
                    peopleCount = response.peopleCount,
                )
            }
}
