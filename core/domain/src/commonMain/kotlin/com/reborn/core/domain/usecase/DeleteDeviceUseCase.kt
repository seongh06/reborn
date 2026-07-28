package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository

class DeleteDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> =
        deviceRepository.deleteDevice(deviceId)
}
