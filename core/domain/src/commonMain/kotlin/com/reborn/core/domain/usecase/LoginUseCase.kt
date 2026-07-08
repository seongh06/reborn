package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.Login

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(loginData: Login): Result<Unit> {
        return authRepository.login(loginData.provider,loginData.token)
    }
}