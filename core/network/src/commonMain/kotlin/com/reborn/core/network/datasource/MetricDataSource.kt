package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse

interface MetricDataSource {
    suspend fun getCurrent(deviceId: String): ApiResponse<MetricCurrentResponse>
}
