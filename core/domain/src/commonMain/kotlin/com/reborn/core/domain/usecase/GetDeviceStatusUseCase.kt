package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.DeviceStatus

class GetDeviceStatusUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<DeviceStatus> = deviceRepository.getStatus(deviceId)
}
