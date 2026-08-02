package com.reborn.core.network.remote

import com.reborn.core.network.datasource.FeedbackDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.feedback.FeedbackStatusUpdateRequest
import com.reborn.core.network.model.response.feedback.FeedbackListResponse
import com.reborn.core.network.model.response.feedback.FeedbackStatusUpdateResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

class FeedbackDataSourceImpl(
    private val httpClient: HttpClient,
) : FeedbackDataSource {

    override suspend fun getList(placeId: Long): ApiResponse<FeedbackListResponse> = runCatching {
        httpClient.get("/api/feedback") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()

    override suspend fun updateStatus(
        feedbackId: Long,
        request: FeedbackStatusUpdateRequest,
    ): ApiResponse<FeedbackStatusUpdateResponse> = runCatching {
        httpClient.patch("/api/feedback/$feedbackId") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun markRead(feedbackId: Long): ApiResponse<Unit?> = runCatching {
        httpClient.patch("/api/feedback/$feedbackId/read")
    }.asApiResponse()
}
