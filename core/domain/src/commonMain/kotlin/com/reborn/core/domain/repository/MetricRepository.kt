package com.reborn.core.domain.repository

import com.reborn.core.model.Metric

interface MetricRepository {
    suspend fun getCurrent(deviceId: String): Result<Metric>
}
