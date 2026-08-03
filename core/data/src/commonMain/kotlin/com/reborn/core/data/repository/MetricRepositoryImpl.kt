package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.Metric
import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.model.request.metric.MetricCollectRequest

class MetricRepositoryImpl(
    private val remote: MetricDataSource,
) : MetricRepository {

    override suspend fun collect(deviceId: String, illuminance: Int, peopleCount: Int): Result<Unit> =
        remote.collect(deviceId, MetricCollectRequest(illuminance = illuminance, peopleCount = peopleCount))
            .toResult { }

    override suspend fun getCurrent(deviceId: String): Result<Metric> =
        remote.getCurrent(deviceId)
            .toResult { response ->
                Metric(
                    temperature = response.temperature,
                    humidity = response.humidity,
                    illuminance = response.illuminance,
                    peopleCount = response.peopleCount,
                    createdAt = response.createdAt,
                )
            }
}
