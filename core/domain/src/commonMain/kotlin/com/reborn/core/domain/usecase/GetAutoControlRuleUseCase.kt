package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.AutoControlRule

class GetAutoControlRuleUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<AutoControlRule?> =
        deviceRepository.getAutoControlRule(deviceId)
}
