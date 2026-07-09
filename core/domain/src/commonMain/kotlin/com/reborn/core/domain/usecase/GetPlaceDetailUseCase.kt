package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.PlaceDetail

class GetPlaceDetailUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<PlaceDetail> {
        return placeRepository.getDetail(placeId)
    }
}
