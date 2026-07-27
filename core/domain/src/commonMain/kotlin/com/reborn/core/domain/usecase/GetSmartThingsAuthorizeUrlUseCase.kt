package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.SmartThingsRepository

class GetSmartThingsAuthorizeUrlUseCase(
    private val smartThingsRepository: SmartThingsRepository
) {
    suspend operator fun invoke(placeId: Long): Result<String> = smartThingsRepository.getAuthorizeUrl(placeId)
}
