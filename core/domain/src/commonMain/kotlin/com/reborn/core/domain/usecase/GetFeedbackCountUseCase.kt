package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.FeedbackRepository
import com.reborn.core.model.FeedbackCount

class GetFeedbackCountUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(placeId: Long): Result<FeedbackCount> {
        return feedbackRepository.getCount(placeId)
    }
}
