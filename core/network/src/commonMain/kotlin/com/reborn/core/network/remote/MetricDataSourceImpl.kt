package com.reborn.core.network.remote

import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class MetricDataSourceImpl(
    private val httpClient: HttpClient,
) : MetricDataSource {

    override suspend fun getCurrent(deviceId: String): ApiResponse<MetricCurrentResponse> = runCatching {
        httpClient.get("/api/metric/current") {
            parameter("deviceId", deviceId)
        }
    }.asApiResponse()

    override suspend fun getHistory(deviceId: String, size: Int): ApiResponse<MetricHistoryResponse> = runCatching {
        httpClient.get("/api/metric/history") {
            parameter("deviceId", deviceId)
            parameter("size", size)
        }
    }.asApiResponse()
}
