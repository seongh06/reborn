package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository

class GetLocalDeviceIdUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(): String? = deviceRepository.getLocalDeviceId()
}
