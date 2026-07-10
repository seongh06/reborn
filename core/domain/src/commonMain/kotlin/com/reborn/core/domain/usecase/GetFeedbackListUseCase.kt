package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.FeedbackRepository
import com.reborn.core.model.Feedback

data class GetFeedbackListParams(val placeId: Long, val status: String? = null)

class GetFeedbackListUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(params: GetFeedbackListParams): Result<List<Feedback>> {
        return feedbackRepository.getList(params.placeId, params.status)
    }
}
