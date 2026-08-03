package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository

class DeleteScheduleRuleUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(ruleId: Long): Result<Unit> =
        deviceRepository.deleteScheduleRule(ruleId)
}
