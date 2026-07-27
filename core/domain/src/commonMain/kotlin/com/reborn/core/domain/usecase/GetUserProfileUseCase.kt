package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.UserProfile

class GetUserProfileUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserProfile> = authRepository.getMe()
}
