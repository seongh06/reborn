package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse

interface MetricDataSource {
    suspend fun getCurrent(deviceId: String): ApiResponse<MetricCurrentResponse>

    suspend fun getHistory(deviceId: String, page: Int, size: Int): ApiResponse<MetricHistoryResponse>
}
