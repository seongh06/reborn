package com.reborn.core.data.repository

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.model.LoginResult
import com.reborn.core.model.UserProfile
import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.di.AuthTokenCacheInvalidator
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.request.auth.UpdateProfileRequest

class AuthRepositoryImpl(
    private val remote: AuthDataSource,
    private val local: AuthLocalDataSource,
    private val tokenCacheInvalidator: AuthTokenCacheInvalidator,
) : AuthRepository {
    override suspend fun login(provider: String, token: String): Result<LoginResult> =
        remote.login(LoginRequest(provider, token))
            .toResult()
            .mapCatching { dto ->
                local.saveTokens(dto.accessToken, dto.refreshToken)
                // Ktor의 Bearer 캐시가 이전 세션(탈퇴/로그아웃된 사용자)의 토큰을 계속 들고 있지
                // 않도록, 새 토큰을 저장한 직후 캐시를 비워서 다음 요청부터 방금 저장한 값을
                // 다시 읽게 한다(#251).
                tokenCacheInvalidator.invalidate()
                LoginResult(userId = dto.userId, name = dto.name, isNewUser = dto.isNewUser)
            }

    // 원격 로그아웃(Redis RefreshToken 삭제)이 401/네트워크 오류로 실패해도
    // 로컬 세션은 항상 종료한다 - 그렇지 않으면 사용자가 로그아웃을 못 하는 상태로 남는다.
    override suspend fun logout(): Result<Unit> = runCatching {
        val remoteResult = runCatching { remote.logout().toResult() }
        local.clearTokens()
        tokenCacheInvalidator.invalidate()
        remoteResult.getOrNull()?.onFailure { println("AuthRepositoryImpl: 원격 로그아웃 실패(로컬 세션은 종료됨) - ${it.message}") }
    }

    override suspend fun updateFcmToken(fcmToken: String): Result<Unit> =
        remote.updateFcmToken(FcmTokenUpdateRequest(fcmToken))
            .toResult()
            .mapCatching { }

    override suspend fun getMe(): Result<UserProfile> =
        remote.getMe()
            .toResult()
            .mapCatching { dto -> UserProfile(userId = dto.userId, name = dto.name, profileImage = dto.profileImage) }

    override suspend fun updateProfile(name: String): Result<UserProfile> =
        remote.updateProfile(UpdateProfileRequest(name))
            .toResult()
            .mapCatching { dto -> UserProfile(userId = dto.userId, name = dto.name, profileImage = dto.profileImage) }

    override suspend fun updateProfileImage(bytes: ByteArray, fileName: String, mimeType: String): Result<UserProfile> =
        remote.updateProfileImage(bytes, fileName, mimeType)
            .toResult()
            .mapCatching { dto -> UserProfile(userId = dto.userId, name = dto.name, profileImage = dto.profileImage) }

    // 실패(예: 유일 관리자라 차단됨) 시에는 로컬 세션을 유지해야 사용자가 오류를 보고 재시도할 수 있다 -
    // logout()과 달리 성공했을 때만 토큰을 지운다.
    override suspend fun withdraw(): Result<Unit> =
        remote.withdraw()
            .toResult()
            .mapCatching {
                local.clearTokens()
                tokenCacheInvalidator.invalidate()
            }
}
