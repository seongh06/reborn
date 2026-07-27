package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository

class ControlDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        isPowerOn: Boolean? = null,
        operationMode: String? = null,
        windSpeed: String? = null,
        temperature: Int? = null,
    ): Result<Unit> {
        return deviceRepository.controlDevice(deviceId, isPowerOn, operationMode, windSpeed, temperature)
    }
}
