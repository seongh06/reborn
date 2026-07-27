package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.SmartThingsRepository
import com.reborn.core.model.RegisteredDevice

class RegisterSmartThingsDeviceUseCase(
    private val smartThingsRepository: SmartThingsRepository
) {
    suspend operator fun invoke(
        placeId: Long,
        smartThingsDeviceId: String,
        deviceName: String,
    ): Result<RegisteredDevice> =
        smartThingsRepository.registerDevice(placeId, smartThingsDeviceId, deviceName)
}
