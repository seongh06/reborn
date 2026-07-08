package com.reborn.core.data.datasource

import com.reborn.core.datastore.TokenLocalDataSource

class AuthLocalDataSourceImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
) : AuthLocalDataSource {

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        tokenLocalDataSource.saveTokens(accessToken, refreshToken)
    }

    override suspend fun clearTokens() {
        tokenLocalDataSource.clear()
    }

    override suspend fun getAccessToken(): String? = tokenLocalDataSource.getAccessToken()

    override suspend fun getRefreshToken(): String? = tokenLocalDataSource.getRefreshToken()
}
