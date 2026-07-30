package com.reborn.core.data.datasource

import kotlinx.coroutines.flow.Flow

interface AuthLocalDataSource {
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    val tutorialCompleted: Flow<Boolean>
    suspend fun setTutorialCompleted(completed: Boolean)
}
