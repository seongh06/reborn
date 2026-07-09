package com.reborn.core.network.remote

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.response.auth.LoginResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class AuthDataSourceImpl(
private val httpClient: HttpClient
): AuthDataSource {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> = runCatching {
        httpClient.post("/api/auth/login") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun logout(accessToken: String): ApiResponse<Unit?> = runCatching {
        httpClient.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }.asApiResponse()

    override suspend fun updateFcmToken(
        accessToken: String,
        request: FcmTokenUpdateRequest,
    ): ApiResponse<Unit?> = runCatching {
        httpClient.patch("/api/auth/fcm") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(request)
        }
    }.asApiResponse()
}