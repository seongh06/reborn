package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.response.auth.LoginResponse

interface AuthDataSource {
    suspend fun login(request: LoginRequest): ApiResponse<LoginResponse>
    suspend fun logout(accessToken: String): ApiResponse<Unit?>
    suspend fun updateFcmToken(accessToken: String, request: FcmTokenUpdateRequest): ApiResponse<Unit?>
}