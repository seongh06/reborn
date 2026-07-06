package com.reborn.server.domain.data.converter

import com.reborn.server.domain.data.SensorLogs
import com.reborn.server.domain.data.dto.SensorDataDto
import com.reborn.server.domain.device.Device
import org.springframework.data.domain.Page

object SensorDataConverter {

    fun toCollectResponse(entity: SensorLogs): SensorDataDto.CollectResponse =
        SensorDataDto.CollectResponse(
            logId = entity.id,
            discomfort = calculateDiscomfort(entity.temperature, entity.humidity),
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toCurrentResponse(device: Device, entity: SensorLogs): SensorDataDto.CurrentResponse =
        SensorDataDto.CurrentResponse(
            deviceId = device.deviceKey,
            deviceName = device.name,
            temperature = entity.temperature,
            humidity = entity.humidity,
            illuminance = entity.illuminance,
            peopleCount = entity.occupancy,
            discomfort = calculateDiscomfort(entity.temperature, entity.humidity),
            createdAt = requireNotNull(entity.createdAt),
        )

    fun toHistoryResponse(deviceId: String, logs: Page<SensorLogs>): SensorDataDto.HistoryResponse =
        SensorDataDto.HistoryResponse(
            deviceId = deviceId,
            logs = logs.content.map(::toHistoryItem),
            page = logs.number,
            size = logs.size,
            totalElements = logs.totalElements,
            totalPages = logs.totalPages,
        )

    private fun toHistoryItem(entity: SensorLogs): SensorDataDto.HistoryItem =
        SensorDataDto.HistoryItem(
            logId = entity.id,
            temperature = entity.temperature,
            humidity = entity.humidity,
            illuminance = entity.illuminance,
            peopleCount = entity.occupancy,
            createdAt = requireNotNull(entity.createdAt),
        )

    // 기상청 불쾌지수 공식: DI = 1.8*T - 0.55*(1 - RH/100)*(1.8*T - 26) + 32
    private fun calculateDiscomfort(temperature: Double?, humidity: Double?): Double? {
        if (temperature == null || humidity == null) return null
        val discomfort = 1.8 * temperature - 0.55 * (1 - humidity / 100) * (1.8 * temperature - 26) + 32
        return Math.round(discomfort * 100) / 100.0
    }
}
