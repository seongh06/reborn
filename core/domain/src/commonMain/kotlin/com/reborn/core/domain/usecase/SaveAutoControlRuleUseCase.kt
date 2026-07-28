package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.AutoControlRule

class SaveAutoControlRuleUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String, rule: AutoControlRule): Result<AutoControlRule> =
        deviceRepository.saveAutoControlRule(deviceId, rule)
}
