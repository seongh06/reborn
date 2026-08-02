package com.reborn.core.domain.repository

import com.reborn.core.model.Feedback

interface FeedbackRepository {
    suspend fun getList(placeId: Long): Result<List<Feedback>>
    suspend fun updateStatus(feedbackId: Long, status: String): Result<Boolean>
    // 승인/거절(status)과는 별도 축 - 관리자가 상세를 열었을 때 읽음 처리(#318)
    suspend fun markRead(feedbackId: Long): Result<Unit>
}
