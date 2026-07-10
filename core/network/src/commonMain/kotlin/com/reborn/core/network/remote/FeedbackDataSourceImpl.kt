package com.reborn.core.network.remote

import com.reborn.core.network.datasource.FeedbackDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.feedback.StatusUpdateRequest
import com.reborn.core.network.model.response.feedback.FeedbackCountResponse
import com.reborn.core.network.model.response.feedback.FeedbackListResponse
import com.reborn.core.network.model.response.feedback.StatusUpdateResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

// "auth" 클라이언트(#121) 사용 - Authorization 헤더는 Auth 플러그인이 자동으로 붙인다.
class FeedbackDataSourceImpl(
    private val httpClient: HttpClient,
) : FeedbackDataSource {

    override suspend fun getList(placeId: Long, status: String?): ApiResponse<FeedbackListResponse> = runCatching {
        httpClient.get("/api/feedback") {
            parameter("placeId", placeId)
            parameter("status", status)
        }
    }.asApiResponse()

    override suspend fun getCount(placeId: Long): ApiResponse<FeedbackCountResponse> = runCatching {
        httpClient.get("/api/feedback/count") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun updateStatus(
        feedbackId: Long,
        request: StatusUpdateRequest,
    ): ApiResponse<StatusUpdateResponse> = runCatching {
        httpClient.patch("/api/feedback/$feedbackId") {
            setBody(request)
        }
    }.asApiResponse()
}
