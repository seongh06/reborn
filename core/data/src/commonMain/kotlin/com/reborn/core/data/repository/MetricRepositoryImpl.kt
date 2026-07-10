package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toMetricHistoryPage
import com.reborn.core.data.mapper.toMetricSnapshot
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.MetricRepository
import com.reborn.core.model.MetricHistoryPage
import com.reborn.core.model.MetricSnapshot
import com.reborn.core.network.datasource.MetricDataSource

class MetricRepositoryImpl(
    private val remote: MetricDataSource,
) : MetricRepository {

    override suspend fun getCurrent(deviceId: String): Result<MetricSnapshot> =
        remote.getCurrent(deviceId)
            .toResult { it.toMetricSnapshot() }

    override suspend fun getHistory(deviceId: String, page: Int, size: Int): Result<MetricHistoryPage> =
        remote.getHistory(deviceId, page, size)
            .toResult { it.toMetricHistoryPage() }
}
