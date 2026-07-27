package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceProvisioningRepository

class ConfigureDeviceWifiUseCase(
    private val deviceProvisioningRepository: DeviceProvisioningRepository
) {
    suspend operator fun invoke(ssid: String, password: String, deviceId: String): Result<Unit> {
        return deviceProvisioningRepository.configureWifi(ssid, password, deviceId)
    }
}
