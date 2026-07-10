package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.Device

class GetDeviceListUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(placeId: Long): Result<List<Device>> {
        return deviceRepository.getList(placeId)
    }
}
