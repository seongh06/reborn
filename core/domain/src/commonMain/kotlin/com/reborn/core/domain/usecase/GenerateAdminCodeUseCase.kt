package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.AdminInviteCode

class GenerateAdminCodeUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<AdminInviteCode> {
        return placeRepository.generateAdminCode(placeId)
    }
}
