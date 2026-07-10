package com.reborn.server.domain.place.service

import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.device.repository.DeviceRepository
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
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.util.generateRandomCode
import com.reborn.server.global.util.generateUuid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PlaceService(
    private val userRepository: UserRepository,
    private val placeRepository: PlaceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val deviceRepository: DeviceRepository,
    private val redisUtil: RedisUtil,
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

    fun generateAdminCode(userId: Long, placeId: Long): PlaceDto.AdminCodeResponse {
        requireAdmin(userId, placeId)

        val code = reserveUniqueCode(ADMIN_INVITE_PREFIX, ADMIN_CODE_LENGTH, placeId.toString())

        return PlaceDto.AdminCodeResponse(
            adminCode = code,
            expiresAt = LocalDateTime.now().plusMinutes(ADMIN_INVITE_TTL_MINUTES),
        )
    }

    @Transactional
    fun redeemAdminCode(userId: Long, request: PlaceDto.AdminInviteRequest): PlaceDto.AdminInviteResponse {
        val code = request.adminCode?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "관리자 코드는 필수입니다.")

        val redisKey = "$ADMIN_INVITE_PREFIX$code"
        val placeId = redisUtil.get(redisKey)?.toLongOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "관리자 코드가 만료되었거나 유효하지 않습니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }

        if (userPlaceMappingRepository.existsByUserIdAndPlaceId(userId, placeId)) {
            throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 해당 장소의 관리자로 등록되어 있습니다.")
        }
        redisUtil.delete(redisKey)

        userPlaceMappingRepository.save(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN))

        return PlaceDto.AdminInviteResponse(
            placeId = place.id,
            placeName = place.name,
            accessLevel = AccessLevel.ADMIN.name,
        )
    }

    fun getList(userId: Long): PlaceDto.ListResponse =
        PlaceDto.ListResponse(
            places = userPlaceMappingRepository.findAllByUserId(userId).map(PlaceConverter::toPlaceItem),
        )

    fun getDetail(userId: Long, placeId: Long): PlaceDto.DetailResponse {
        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
            ?: throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "권한이 없습니다.")

        val deviceCount = deviceRepository.countByPlaceId(placeId).toInt()
        return PlaceConverter.toDetailResponse(place, mapping.accessLevel, deviceCount)
    }

    @Transactional
    fun deletePlace(userId: Long, placeId: Long) {
        requireAdmin(userId, placeId)
        placeRepository.deleteByIdInBulk(placeId)
    }

    private fun requireAdmin(userId: Long, placeId: Long): Place {
        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
        if (mapping == null || mapping.accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
        return place
    }

    private fun reserveUniqueCode(prefix: String, length: Int, value: String): String {
        repeat(MAX_CODE_GENERATION_ATTEMPTS) {
            val code = generateRandomCode(length)
            if (redisUtil.setIfAbsent("$prefix$code", value, Duration.ofMinutes(ADMIN_INVITE_TTL_MINUTES))) {
                return code
            }
        }
        throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "코드 생성에 실패했습니다. 다시 시도해주세요.")
    }

    private fun parsePlaceType(type: String?): PlaceType {
        if (type.isNullOrBlank()) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "공간 유형은 필수입니다.")
        }
        return runCatching { PlaceType.valueOf(type) }
            .getOrElse { throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "정의되지 않은 공간 유형입니다.") }
    }

    companion object {
        private const val ADMIN_INVITE_PREFIX = "admin-invite:"
        private const val ADMIN_CODE_LENGTH = 6
        private const val ADMIN_INVITE_TTL_MINUTES = 30L
        private const val MAX_CODE_GENERATION_ATTEMPTS = 5
    }
}
