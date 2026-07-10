package com.reborn.core.domain.repository

import com.reborn.core.model.Feedback
import com.reborn.core.model.FeedbackCount

interface FeedbackRepository {
    suspend fun getList(placeId: Long, status: String? = null): Result<List<Feedback>>

    suspend fun getCount(placeId: Long): Result<FeedbackCount>

    suspend fun updateStatus(feedbackId: Long, status: String): Result<Unit>
}
