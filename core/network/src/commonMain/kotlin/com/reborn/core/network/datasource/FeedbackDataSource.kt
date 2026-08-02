package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.feedback.FeedbackStatusUpdateRequest
import com.reborn.core.network.model.response.feedback.FeedbackListResponse
import com.reborn.core.network.model.response.feedback.FeedbackStatusUpdateResponse

interface FeedbackDataSource {
    suspend fun getList(placeId: Long): ApiResponse<FeedbackListResponse>
    suspend fun updateStatus(
        feedbackId: Long,
        request: FeedbackStatusUpdateRequest,
    ): ApiResponse<FeedbackStatusUpdateResponse>

    // 승인/거절(status)과는 별도 축 - 관리자가 상세를 열었을 때 읽음 처리(#318)
    suspend fun markRead(feedbackId: Long): ApiResponse<Unit?>
}
