package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.AuthRepository

class WithdrawUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.withdraw()
}
