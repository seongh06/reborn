package com.reborn.core.data.datasource

import kotlinx.coroutines.flow.Flow

interface AuthLocalDataSource {
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    val tutorialSeenSteps: Flow<Set<String>>
    suspend fun markTutorialStepSeen(stepId: String)
}
