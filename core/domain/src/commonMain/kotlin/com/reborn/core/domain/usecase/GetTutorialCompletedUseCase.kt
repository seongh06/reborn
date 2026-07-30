package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class GetTutorialCompletedUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.getTutorialCompleted()
}
