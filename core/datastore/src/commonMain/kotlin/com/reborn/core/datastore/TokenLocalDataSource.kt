package com.reborn.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TokenLocalDataSource(
    private val dataStore: DataStore<AuthTokens>,
) {
    val authTokens: Flow<AuthTokens> = dataStore.data

    suspend fun getAccessToken(): String? = dataStore.data.first().accessToken

    suspend fun getRefreshToken(): String? = dataStore.data.first().refreshToken

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.updateData { it.copy(accessToken = accessToken, refreshToken = refreshToken) }
    }

    suspend fun getDeviceId(): String? = dataStore.data.first().deviceId

    suspend fun getAppToken(): String? = dataStore.data.first().appToken

    suspend fun isAerometerDevice(): Boolean = dataStore.data.first().isAerometer

    // 페어링 성공 시 자격증명 저장과 동시에 "이 인스턴스는 공기계다"라는 상태도 함께 기록한다.
    suspend fun saveDeviceCredentials(deviceId: String, appToken: String) {
        dataStore.updateData { it.copy(deviceId = deviceId, appToken = appToken, isAerometer = true) }
    }

    suspend fun clear() {
        dataStore.updateData { AuthTokens() }
    }
}
