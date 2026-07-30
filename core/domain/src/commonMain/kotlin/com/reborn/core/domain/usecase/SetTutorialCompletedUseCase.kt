package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository

class SetTutorialCompletedUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(completed: Boolean): Result<Unit> =
        authRepository.setTutorialCompleted(completed)
}
