package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.Place

class RegisterPlaceUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(name: String, type: String): Result<Place> {
        return placeRepository.register(name, type)
    }
}
