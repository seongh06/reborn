package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.PairingCode

class GeneratePairingCodeUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<PairingCode> {
        return deviceRepository.generatePairingCode(placeId)
    }
}
