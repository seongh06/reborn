package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.request.auth.UpdateProfileRequest
import com.reborn.core.network.model.response.auth.LoginResponse
import com.reborn.core.network.model.response.auth.MeResponse

interface AuthDataSource {
    suspend fun login(request: LoginRequest): ApiResponse<LoginResponse>
    suspend fun logout(): ApiResponse<Unit?>
    suspend fun updateFcmToken(request: FcmTokenUpdateRequest): ApiResponse<Unit?>
    suspend fun getMe(): ApiResponse<MeResponse>
    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<MeResponse>
    suspend fun updateProfileImage(bytes: ByteArray, fileName: String, mimeType: String): ApiResponse<MeResponse>
    suspend fun withdraw(): ApiResponse<Unit?>
}