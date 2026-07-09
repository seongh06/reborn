package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository

class UpdateFcmTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(fcmToken: String): Result<Unit> {
        return authRepository.updateFcmToken(fcmToken)
    }
}
