package com.reborn.server.domain.data

import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SensorDataService(
    private val deviceRepository: DeviceRepository,
    private val sensorLogsRepository: SensorLogsRepository,
) {

    @Transactional
    fun collect(deviceId: String, request: SensorDataDto.CollectRequest): SensorDataDto.CollectResponse {
        validateCollectRequest(request)

        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기입니다.")

        val sensorLog = sensorLogsRepository.save(
            SensorLogs(
                device = device,
                temperature = request.temperature,
                humidity = request.humidity,
                illuminance = request.illuminance,
                occupancy = request.peopleCount,
            ),
        )
        device.updateOnlineStatus(true)

        return SensorDataConverter.toCollectResponse(sensorLog)
    }

    fun getCurrent(deviceId: String): SensorDataDto.CurrentResponse {
        val device = deviceRepository.findByDeviceKey(deviceId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기이거나 수집된 데이터가 없습니다.")

        val latestLog = sensorLogsRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.id)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "등록되지 않은 기기이거나 수집된 데이터가 없습니다.")

        return SensorDataConverter.toCurrentResponse(device, latestLog)
    }

    private fun validateCollectRequest(request: SensorDataDto.CollectRequest) {
        if (request.temperature == null && request.humidity == null && request.illuminance == null && request.peopleCount == null) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "수집된 센서 데이터가 없습니다.")
        }
        if (request.humidity != null && request.humidity !in 0.0..100.0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "습도는 0~100 사이의 값이어야 합니다.")
        }
        if (request.illuminance != null && request.illuminance < 0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "조도는 0 이상의 값이어야 합니다.")
        }
        if (request.peopleCount != null && request.peopleCount < 0) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "재실 인원은 0 이상의 값이어야 합니다.")
        }
    }
}
