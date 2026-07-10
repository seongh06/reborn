package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.FeedbackRepository

class UpdateFeedbackStatusUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(feedbackId: Long, status: String): Result<Unit> {
        return feedbackRepository.updateStatus(feedbackId, status)
    }
}
