package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.PlaceMembership

class RedeemAdminCodeUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(adminCode: String): Result<PlaceMembership> {
        return placeRepository.redeemAdminCode(adminCode)
    }
}
