package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.FeedbackRepository
import com.reborn.core.model.Feedback

class GetFeedbackListUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(placeId: Long): Result<List<Feedback>> = feedbackRepository.getList(placeId)
}
