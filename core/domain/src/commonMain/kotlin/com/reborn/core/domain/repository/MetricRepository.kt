package com.reborn.core.domain.repository

import com.reborn.core.model.MetricHistoryPage
import com.reborn.core.model.MetricSnapshot

interface MetricRepository {
    suspend fun getCurrent(deviceId: String): Result<MetricSnapshot>

    suspend fun getHistory(deviceId: String, page: Int, size: Int): Result<MetricHistoryPage>
}
