package com.reborn.core.data.repository

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.LoginResult
import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.request.auth.LoginRequest

class AuthRepositoryImpl(
    private val remote: AuthDataSource,
    private val local: AuthLocalDataSource,
) : AuthRepository {
    override suspend fun login(provider: String, token: String): Result<LoginResult> =
        remote.login(LoginRequest(provider, token))
            .toResult()
            .mapCatching { dto ->
                local.saveTokens(dto.accessToken, dto.refreshToken)
                LoginResult(userId = dto.userId, name = dto.name, isNewUser = dto.isNewUser)
            }
}
