package com.reborn.core.network.remote

import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricAggregateResponse
import com.reborn.core.network.model.response.metric.MetricAnalysisResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricExportResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

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

    override suspend fun getAggregate(deviceId: String, period: String): ApiResponse<MetricAggregateResponse> = runCatching {
        httpClient.get("/api/metric/aggregate") {
            parameter("deviceId", deviceId)
            parameter("period", period)
        }
    }.asApiResponse()

    override suspend fun getAnalysis(deviceId: String, category: String): ApiResponse<MetricAnalysisResponse> = runCatching {
        httpClient.get("/api/metric/analysis") {
            parameter("deviceId", deviceId)
            parameter("category", category)
        }
    }.asApiResponse()

    override suspend fun exportToSheets(deviceId: String): ApiResponse<MetricExportResponse> = runCatching {
        httpClient.post("/api/metric/export") {
            parameter("deviceId", deviceId)
        }
    }.asApiResponse()
}
