package com.reborn.server.domain.device.service

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.converter.DeviceConverter
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeviceService(
    private val placeRepository: PlaceRepository,
    private val deviceRepository: DeviceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
) {

    @Transactional
    fun register(userId: Long, request: DeviceDto.RegisterRequest): DeviceDto.RegisterResponse {
        val placeId = request.placeId
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "장소 ID는 필수입니다.")
        val deviceKey = request.deviceId?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 ID는 필수입니다.")
        val deviceName = request.deviceName?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "기기 이름은 필수입니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
        if (mapping == null || mapping.accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }

        if (deviceRepository.existsByDeviceKey(deviceKey)) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        val device = try {
            deviceRepository.save(
                Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = deviceKey, name = deviceName),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 등록된 기기입니다.")
        }

        return DeviceConverter.toRegisterResponse(device)
    }
}
