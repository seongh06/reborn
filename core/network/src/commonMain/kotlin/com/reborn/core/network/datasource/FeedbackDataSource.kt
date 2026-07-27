package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.feedback.FeedbackStatusUpdateRequest
import com.reborn.core.network.model.response.feedback.FeedbackListResponse
import com.reborn.core.network.model.response.feedback.FeedbackStatusUpdateResponse

interface FeedbackDataSource {
    suspend fun getList(placeId: Long): ApiResponse<FeedbackListResponse>
    suspend fun updateStatus(feedbackId: Long, request: FeedbackStatusUpdateRequest): ApiResponse<FeedbackStatusUpdateResponse>
}
