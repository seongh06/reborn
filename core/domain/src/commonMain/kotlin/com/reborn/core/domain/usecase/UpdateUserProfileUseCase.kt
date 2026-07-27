package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.UserProfile

class UpdateUserProfileUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String): Result<UserProfile> = authRepository.updateProfile(name)
}
