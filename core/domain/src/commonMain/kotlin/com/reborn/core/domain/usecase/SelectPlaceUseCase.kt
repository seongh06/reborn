package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository

class SelectPlaceUseCase(
    private val placeRepository: PlaceRepository,
) {
    operator fun invoke(placeId: Long) {
        placeRepository.selectPlace(placeId)
    }
}
