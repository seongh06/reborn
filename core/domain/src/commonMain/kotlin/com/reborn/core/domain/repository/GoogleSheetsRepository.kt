package com.reborn.core.domain.repository

interface GoogleSheetsRepository {
    suspend fun getAuthorizeUrl(placeId: Long): Result<String>
}
