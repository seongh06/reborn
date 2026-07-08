package com.reborn.core.domain.repository

interface AuthRepository {
    suspend fun login(provider: String, token: String): Result<Unit>
}
