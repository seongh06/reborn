package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.Place

class GetPlaceListUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(): Result<List<Place>> {
        return placeRepository.getList()
    }
}
