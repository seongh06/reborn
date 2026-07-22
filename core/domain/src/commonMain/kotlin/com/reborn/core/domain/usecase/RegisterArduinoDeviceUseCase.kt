package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.RegisteredDevice

class RegisterArduinoDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(placeId: Long, deviceId: String, deviceName: String): Result<RegisteredDevice> {
        return deviceRepository.registerDevice(placeId, deviceId, deviceName)
    }
}
