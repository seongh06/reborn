package com.reborn.core.data.repository

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.LoginResult
import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
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

    // 원격 로그아웃(Redis RefreshToken 삭제)이 401/네트워크 오류로 실패해도
    // 로컬 세션은 항상 종료한다 - 그렇지 않으면 사용자가 로그아웃을 못 하는 상태로 남는다.
    override suspend fun logout(): Result<Unit> = runCatching {
        val remoteResult = runCatching { remote.logout().toResult() }
        local.clearTokens()
        remoteResult.getOrNull()?.onFailure { println("AuthRepositoryImpl: 원격 로그아웃 실패(로컬 세션은 종료됨) - ${it.message}") }
    }

    override suspend fun updateFcmToken(fcmToken: String): Result<Unit> =
        remote.updateFcmToken(FcmTokenUpdateRequest(fcmToken))
            .toResult()
            .mapCatching { }
}
