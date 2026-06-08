package com.reborn.server.domain.place.service

import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.place.converter.PlaceConverter
import com.reborn.server.domain.place.dto.PlaceDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.util.generateUuid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val userRepository: UserRepository,
    private val placeRepository: PlaceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
) {

    @Transactional
    fun register(userId: Long, request: PlaceDto.RegisterRequest): PlaceDto.RegisterResponse {
        val name = request.name?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "장소 이름은 필수입니다.")
        val type = parsePlaceType(request.type)

        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }

        val place = placeRepository.save(Place(name = name, qrCode = generateUuid(), type = type))
        userPlaceMappingRepository.save(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN))

        return PlaceConverter.toRegisterResponse(place)
    }

    private fun parsePlaceType(type: String?): PlaceType {
        if (type.isNullOrBlank()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "공간 유형은 필수입니다.")
        }
        return runCatching { PlaceType.valueOf(type) }
            .getOrElse { throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "정의되지 않은 공간 유형입니다.") }
    }
}
