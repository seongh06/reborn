package com.reborn.core.domain.repository

import com.reborn.core.model.LoginResult

interface AuthRepository {
    suspend fun login(provider: String, token: String): Result<LoginResult>
    suspend fun logout(): Result<Unit>
}
