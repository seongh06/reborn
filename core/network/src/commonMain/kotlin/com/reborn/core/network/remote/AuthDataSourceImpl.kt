package com.reborn.core.network.remote

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.response.auth.LoginResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// "auth" 클라이언트(#121) 사용 - Authorization 헤더는 Auth 플러그인이 자동으로 붙인다.
// login은 아직 토큰이 없어 자연히 헤더 없이 나간다.
class AuthDataSourceImpl(
private val httpClient: HttpClient
): AuthDataSource {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> = runCatching {
        httpClient.post("/api/auth/login") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun logout(): ApiResponse<Unit?> = runCatching {
        httpClient.post("/api/auth/logout")
    }.asApiResponse()

    override suspend fun updateFcmToken(request: FcmTokenUpdateRequest): ApiResponse<Unit?> = runCatching {
        httpClient.patch("/api/auth/fcm") {
            setBody(request)
        }
    }.asApiResponse()
}