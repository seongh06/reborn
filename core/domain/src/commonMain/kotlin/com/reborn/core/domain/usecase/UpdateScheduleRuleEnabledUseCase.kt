package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.ScheduleRule

class UpdateScheduleRuleEnabledUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(ruleId: Long, enabled: Boolean): Result<ScheduleRule> =
        deviceRepository.updateScheduleRuleEnabled(ruleId, enabled)
}
