package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.GoogleSheetsRepository

class GetGoogleSheetsAuthorizeUrlUseCase(
    private val googleSheetsRepository: GoogleSheetsRepository
) {
    suspend operator fun invoke(placeId: Long): Result<String> = googleSheetsRepository.getAuthorizeUrl(placeId)
}
