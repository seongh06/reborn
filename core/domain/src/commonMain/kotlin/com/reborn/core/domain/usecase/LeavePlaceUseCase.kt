package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository

class LeavePlaceUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<Unit> {
        return placeRepository.leave(placeId)
    }
}
