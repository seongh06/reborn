package com.reborn.core.network.remote

import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.metric.MetricCurrentResponse
import com.reborn.core.network.model.response.metric.MetricHistoryResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

// "auth" 클라이언트(#121) 사용 - getHistory는 인증 필수, getCurrent는 서버에서 permitAll이지만
// 관리자 앱에서는 항상 로그인된 상태로 호출되므로 동일 클라이언트로 통일
class MetricDataSourceImpl(
    private val httpClient: HttpClient,
) : MetricDataSource {

    override suspend fun getCurrent(deviceId: String): ApiResponse<MetricCurrentResponse> = runCatching {
        httpClient.get("/api/metric/current") {
            parameter("deviceId", deviceId)
        }
    }.asApiResponse()

    override suspend fun getHistory(deviceId: String, page: Int, size: Int): ApiResponse<MetricHistoryResponse> =
        runCatching {
            httpClient.get("/api/metric/history") {
                parameter("deviceId", deviceId)
                parameter("page", page)
                parameter("size", size)
            }
        }.asApiResponse()
}
