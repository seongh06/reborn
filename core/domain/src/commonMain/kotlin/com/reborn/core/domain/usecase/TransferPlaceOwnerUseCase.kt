package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository

class TransferPlaceOwnerUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long, newOwnerUserId: Long): Result<Unit> {
        return placeRepository.transferOwner(placeId, newOwnerUserId)
    }
}
