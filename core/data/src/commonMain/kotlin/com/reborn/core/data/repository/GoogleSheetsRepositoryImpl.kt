package com.reborn.core.data.repository

import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.GoogleSheetsRepository
import com.reborn.core.network.datasource.GoogleSheetsDataSource

class GoogleSheetsRepositoryImpl(
    private val remote: GoogleSheetsDataSource,
) : GoogleSheetsRepository {

    override suspend fun getAuthorizeUrl(placeId: Long): Result<String> =
        remote.getAuthorizeUrl(placeId)
            .toResult { it.authorizeUrl }
}
