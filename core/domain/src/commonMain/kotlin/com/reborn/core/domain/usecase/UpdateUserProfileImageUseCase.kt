package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.UserProfile

class UpdateUserProfileImageUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(bytes: ByteArray, fileName: String, mimeType: String): Result<UserProfile> =
        authRepository.updateProfileImage(bytes, fileName, mimeType)
}
