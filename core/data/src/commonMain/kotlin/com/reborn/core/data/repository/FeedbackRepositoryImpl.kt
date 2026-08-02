package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toFeedback
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.FeedbackRepository
import com.reborn.core.model.Feedback
import com.reborn.core.network.datasource.FeedbackDataSource
import com.reborn.core.network.model.request.feedback.FeedbackStatusUpdateRequest

class FeedbackRepositoryImpl(
    private val remote: FeedbackDataSource,
) : FeedbackRepository {

    override suspend fun getList(placeId: Long): Result<List<Feedback>> =
        remote.getList(placeId)
            .toResult { response -> response.feedbacks.map { it.toFeedback() } }

    override suspend fun updateStatus(feedbackId: Long, status: String): Result<Boolean> =
        remote.updateStatus(feedbackId, FeedbackStatusUpdateRequest(status))
            .toResult { response -> response.controlSent }

    override suspend fun markRead(feedbackId: Long): Result<Unit> =
        remote.markRead(feedbackId).toResult { }
}
