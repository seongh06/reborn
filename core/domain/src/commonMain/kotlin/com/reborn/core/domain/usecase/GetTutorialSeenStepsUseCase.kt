package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetTutorialSeenStepsUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Set<String>> = authRepository.getTutorialSeenSteps()
}
