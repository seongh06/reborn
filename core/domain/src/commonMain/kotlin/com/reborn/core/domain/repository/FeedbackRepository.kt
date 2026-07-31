package com.reborn.core.domain.repository

import com.reborn.core.model.Feedback

interface FeedbackRepository {
    suspend fun getList(placeId: Long): Result<List<Feedback>>
    suspend fun updateStatus(feedbackId: Long, status: String): Result<Boolean>
}
