package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.PlaceWifi

class GetPlaceWifiUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<PlaceWifi> {
        return placeRepository.getWifi(placeId)
    }
}
