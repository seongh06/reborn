package com.reborn.core.network.remote

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.response.auth.LoginResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthDataSourceImpl(
private val httpClient: HttpClient
): AuthDataSource {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> = runCatching {
        httpClient.post("/api/auth/login") {
            setBody(request)
        }
    }.asApiResponse()
}