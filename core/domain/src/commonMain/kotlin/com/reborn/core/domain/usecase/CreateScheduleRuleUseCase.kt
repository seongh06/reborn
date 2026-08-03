package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.ScheduleRule

class CreateScheduleRuleUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(
        deviceId: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<String>,
        isPowerOn: Boolean,
        operationMode: String?,
    ): Result<ScheduleRule> =
        deviceRepository.createScheduleRule(deviceId, hour, minute, daysOfWeek, isPowerOn, operationMode)
}
