package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.PlaceAdmin

class GetPlaceAdminsUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<List<PlaceAdmin>> {
        return placeRepository.getAdmins(placeId)
    }
}
