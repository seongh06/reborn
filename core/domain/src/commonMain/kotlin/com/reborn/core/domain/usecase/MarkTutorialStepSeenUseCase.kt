package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository

class MarkTutorialStepSeenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(stepId: String): Result<Unit> =
        authRepository.markTutorialStepSeen(stepId)
}
