package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toFeedback
import com.reborn.core.data.mapper.toFeedbackCount
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.FeedbackRepository
import com.reborn.core.model.Feedback
import com.reborn.core.model.FeedbackCount
import com.reborn.core.network.datasource.FeedbackDataSource
import com.reborn.core.network.model.request.feedback.StatusUpdateRequest

class FeedbackRepositoryImpl(
    private val remote: FeedbackDataSource,
) : FeedbackRepository {

    override suspend fun getList(placeId: Long, status: String?): Result<List<Feedback>> =
        remote.getList(placeId, status)
            .toResult { response -> response.feedbacks.map { it.toFeedback() } }

    override suspend fun getCount(placeId: Long): Result<FeedbackCount> =
        remote.getCount(placeId)
            .toResult { it.toFeedbackCount() }

    override suspend fun updateStatus(feedbackId: Long, status: String): Result<Unit> =
        remote.updateStatus(feedbackId, StatusUpdateRequest(status))
            .toResult { }
}
