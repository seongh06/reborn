package com.reborn.core.domain.repository

import com.reborn.core.model.LoginResult
import com.reborn.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(provider: String, token: String): Result<LoginResult>
    suspend fun logout(): Result<Unit>
    suspend fun updateFcmToken(fcmToken: String): Result<Unit>
    suspend fun getMe(): Result<UserProfile>
    suspend fun updateProfile(name: String): Result<UserProfile>
    suspend fun updateProfileImage(bytes: ByteArray, fileName: String, mimeType: String): Result<UserProfile>
    suspend fun withdraw(): Result<Unit>
    fun getTutorialCompleted(): Flow<Boolean>
    suspend fun setTutorialCompleted(completed: Boolean): Result<Unit>
}
