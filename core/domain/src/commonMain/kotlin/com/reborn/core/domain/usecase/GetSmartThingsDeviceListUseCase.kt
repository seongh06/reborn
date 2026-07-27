package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.SmartThingsRepository
import com.reborn.core.model.SmartThingsDevice

class GetSmartThingsDeviceListUseCase(
    private val smartThingsRepository: SmartThingsRepository
) {
    suspend operator fun invoke(placeId: Long): Result<List<SmartThingsDevice>> =
        smartThingsRepository.getDevices(placeId)
}
