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
        userPlaceMappingRepository.save(
            UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN, isOwner = true),
        )

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
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
            ?: throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "권한이 없습니다.")
        val isOwner = userPlaceMappingRepository.findIsOwnerByUserIdAndPlaceId(userId, placeId) ?: false

        val deviceCount = deviceRepository.countByPlaceId(placeId).toInt()
        val adminCount = userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(placeId, AccessLevel.ADMIN).size
        return PlaceConverter.toDetailResponse(place, accessLevel, isOwner, deviceCount, adminCount)
    }

    // 설정 화면 place 카드에 관리자 프로필(이름/사진)을 보여주기 위한 조회(#217) - 카드에 바로 노출되는
    // 정보라 getDetail의 adminCount(숫자만)와 별개로 실제 목록이 필요해졌다. ADMIN만 조회 가능.
    fun getAdmins(userId: Long, placeId: Long): PlaceDto.AdminListResponse {
        requireAdmin(userId, placeId)
        val admins = userPlaceMappingRepository.findAllByPlaceIdAndAccessLevel(placeId, AccessLevel.ADMIN)
            .map { PlaceConverter.toAdminItem(it) }
        return PlaceDto.AdminListResponse(admins = admins)
    }

    // 아두이노/AI스피커 SoftAP 프로비저닝 화면(#219)에서 이 장소에 저장된 WiFi를 미리 불러와
    // 관리자가 매번 재입력하지 않도록 자동 채움. 저장된 적 없으면 ssid/password 모두 null.
    fun getWifi(userId: Long, placeId: Long): PlaceDto.WifiResponse {
        val place = requireAdmin(userId, placeId)
        return PlaceDto.WifiResponse(ssid = place.wifiSsid, password = place.wifiPassword)
    }

    @Transactional
    fun updateWifi(userId: Long, placeId: Long, request: PlaceDto.WifiRequest): PlaceDto.WifiResponse {
        val ssid = request.ssid?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "WiFi 이름(SSID)은 필수입니다.")
        val place = requireAdmin(userId, placeId)
        place.updateWifi(ssid, request.password.orEmpty())
        return PlaceDto.WifiResponse(ssid = place.wifiSsid, password = place.wifiPassword)
    }

    // 장소 하드 삭제는 방장만 할 수 있다 - 방장이 아닌 관리자는 leavePlace()로 자기 매핑만 나간다.
    @Transactional
    fun deletePlace(userId: Long, placeId: Long) {
        requireOwner(userId, placeId)
        placeRepository.deleteByIdInBulk(placeId)
    }

    // 방장이 아닌 관리자/사용자가 장소에서 스스로 빠지는 API - 장소는 그대로 유지되고 내 매핑만 지운다.
    // 방장은 나갈 수 없다(장소가 방장 없는 상태가 되므로) - 위임 먼저 하거나 삭제해야 한다.
    @Transactional
    fun leavePlace(userId: Long, placeId: Long) {
        val mapping = userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "해당 장소에 속해있지 않습니다.")
        if (mapping.isOwner) {
            throw BusinessAlertException(
                CommonErrorCode.CONFLICT,
                "방장은 장소를 나갈 수 없습니다. 다른 관리자에게 방장을 위임한 뒤 나가거나, 장소를 삭제해주세요.",
            )
        }
        userPlaceMappingRepository.deleteByUserIdAndPlaceId(userId, placeId)
    }

    // 방장 위임 - 현재 방장만 호출 가능, 대상은 같은 장소의 ADMIN이어야 한다.
    @Transactional
    fun transferOwner(
        userId: Long,
        placeId: Long,
        request: PlaceDto.TransferOwnerRequest,
    ): PlaceDto.TransferOwnerResponse {
        val newOwnerUserId = request.newOwnerUserId
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "newOwnerUserId는 필수입니다.")
        requireOwner(userId, placeId)
        if (newOwnerUserId == userId) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이미 방장입니다.")
        }

        // requireOwner()가 이미 userId가 이 장소의 방장임을 확인했으므로 isOwner로 다시 조회할
        // 필요 없이 그 매핑을 그대로 재사용한다(CodeRabbit).
        val currentOwnerMapping = requireNotNull(userPlaceMappingRepository.findByUserIdAndPlaceId(userId, placeId))
        val newOwnerMapping = userPlaceMappingRepository.findByUserIdAndPlaceId(newOwnerUserId, placeId)
            ?.takeIf { it.accessLevel == AccessLevel.ADMIN }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "해당 장소의 관리자만 방장으로 위임할 수 있습니다.")

        currentOwnerMapping.revokeOwner()
        newOwnerMapping.assignOwner()

        return PlaceDto.TransferOwnerResponse(placeId = placeId, newOwnerUserId = newOwnerUserId)
    }

    private fun requireAdmin(userId: Long, placeId: Long): Place {
        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
        return place
    }

    private fun requireOwner(userId: Long, placeId: Long) {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        val isOwner = userPlaceMappingRepository.findIsOwnerByUserIdAndPlaceId(userId, placeId)
        if (isOwner != true) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "방장만 할 수 있습니다.")
        }
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
