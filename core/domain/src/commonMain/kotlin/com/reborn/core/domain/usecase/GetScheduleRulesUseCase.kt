package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.ScheduleRule

class GetScheduleRulesUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<List<ScheduleRule>> =
        deviceRepository.getScheduleRules(deviceId)
}
