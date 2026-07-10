package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.PairedDevice

class PairDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(pairingCode: String, deviceName: String): Result<PairedDevice> {
        return deviceRepository.pairDevice(pairingCode, deviceName)
    }
}
