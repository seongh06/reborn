package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.feedback.StatusUpdateRequest
import com.reborn.core.network.model.response.feedback.FeedbackCountResponse
import com.reborn.core.network.model.response.feedback.FeedbackListResponse
import com.reborn.core.network.model.response.feedback.StatusUpdateResponse

interface FeedbackDataSource {
    suspend fun getList(placeId: Long, status: String?): ApiResponse<FeedbackListResponse>

    suspend fun getCount(placeId: Long): ApiResponse<FeedbackCountResponse>

    suspend fun updateStatus(feedbackId: Long, request: StatusUpdateRequest): ApiResponse<StatusUpdateResponse>
}
