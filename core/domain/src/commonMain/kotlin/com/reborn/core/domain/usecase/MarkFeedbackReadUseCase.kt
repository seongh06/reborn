package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.FeedbackRepository

class MarkFeedbackReadUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(feedbackId: Long): Result<Unit> =
        feedbackRepository.markRead(feedbackId)
}
