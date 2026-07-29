package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.PlaceWifi

class UpdatePlaceWifiUseCase(
    private val placeRepository: PlaceRepository
) {
    suspend operator fun invoke(placeId: Long, ssid: String, password: String): Result<PlaceWifi> {
        return placeRepository.updateWifi(placeId, ssid, password)
    }
}
