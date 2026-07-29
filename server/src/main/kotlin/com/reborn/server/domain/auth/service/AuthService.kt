package com.reborn.server.domain.auth.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.auth.client.GoogleAuthClient
import com.reborn.server.domain.auth.client.KakaoAuthClient
import com.reborn.server.domain.auth.client.SocialUserInfo
import com.reborn.server.domain.auth.converter.AuthConverter
import com.reborn.server.domain.auth.dto.AuthDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.storage.LocalFileStorage
import com.reborn.server.global.token.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.time.Duration

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val kakaoAuthClient: KakaoAuthClient,
    private val jwtProvider: JwtProvider,
    private val redisUtil: RedisUtil,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val localFileStorage: LocalFileStorage,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun login(request: AuthDto.LoginRequest): AuthDto.LoginResponse {
        val token = request.token?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "token은 필수입니다.")
        val provider = parseProvider(request.provider)

        val socialUserInfo = when (provider) {
            OAuthProvider.GOOGLE -> googleAuthClient.verify(token)
            OAuthProvider.KAKAO -> kakaoAuthClient.verify(token)
        }
        return login(provider, socialUserInfo)
    }

    private fun parseProvider(provider: String?): OAuthProvider {
        val parsed = provider?.let { runCatching { OAuthProvider.valueOf(it.trim().uppercase()) }.getOrNull() }
        return parsed ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 provider입니다. (GOOGLE, KAKAO 중 하나를 입력하세요)")
    }

    fun refresh(request: AuthDto.RefreshRequest): AuthDto.RefreshResponse {
        val refreshToken = request.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "refreshToken은 필수입니다.")

        val claims = jwtProvider.parseClaims(refreshToken)
            ?.takeIf { it[JwtProvider.TYPE_KEY] == JwtProvider.REFRESH_TYPE }
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 RefreshToken입니다.")
        val userId = claims.subject?.toLongOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 RefreshToken입니다.")

        val storedToken = redisUtil.get("refresh:$userId")
        if (storedToken == null || storedToken != refreshToken) {
            throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않거나 만료된 RefreshToken입니다.")
        }

        val newAccessToken = jwtProvider.createAccessToken(userId)
        val newRefreshToken = jwtProvider.createRefreshToken(userId)
        redisUtil.set("refresh:$userId", newRefreshToken, Duration.ofMillis(jwtProvider.refreshTokenExpiry))

        return AuthDto.RefreshResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    fun logout(userId: Long) {
        redisUtil.delete("refresh:$userId")
    }

    fun getMe(userId: Long): AuthDto.MeResponse {
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }
        return AuthConverter.toMeResponse(user)
    }

    @Transactional
    fun updateProfile(userId: Long, request: AuthDto.UpdateProfileRequest): AuthDto.MeResponse {
        val name = request.name?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이름은 필수입니다.")
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }
        user.updateName(name)
        return AuthConverter.toMeResponse(user)
    }

    @Transactional
    fun updateProfileImage(userId: Long, file: MultipartFile): AuthDto.MeResponse {
        validateImageFile(file)
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }

        val previousImage = user.profileImage
        val uploaded = localFileStorage.upload(file, directory = "profile")
        user.updateProfileImage(uploaded.url)

        // 이전 이미지가 우리 서버가 직접 서빙하는 파일일 때만(카카오/구글 프로필 URL이 아닐 때만) 정리 -
        // 실패해도 새 이미지 저장 자체는 이미 끝났으니 업로드 응답에는 영향 주지 않는다.
        previousImage?.let(localFileStorage::extractKeyIfOwned)?.let { key ->
            runCatching { localFileStorage.delete(key) }
                .onFailure { log.warn("이전 프로필 이미지 삭제 실패: key={}", key, it) }
        }

        return AuthConverter.toMeResponse(user)
    }

    private fun validateImageFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이미지 파일이 비어있습니다.")
        }
        if (file.contentType !in ALLOWED_IMAGE_CONTENT_TYPES) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 이미지 형식입니다. (JPEG/PNG/WEBP만 허용)")
        }
        if (file.size > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "이미지 파일이 너무 큽니다. (최대 5MB)")
        }
    }

    @Transactional
    fun updateFcmToken(userId: Long, request: AuthDto.FcmTokenUpdateRequest) {
        val fcmToken = request.fcmToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "fcmToken은 필수입니다.")
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }
        user.updateFcmToken(fcmToken)
    }

    // 탈퇴 시 이 사용자가 유일한 ADMIN인 장소가 있으면 그 장소의 관리 주체가 사라지므로 차단한다 -
    // cascade 삭제(장소/기기까지 함께 삭제)는 다른 관리자/사용자의 데이터까지 날리는 위험한 작업이라
    // 채택하지 않고, 사용자가 먼저 다른 관리자를 초대하거나 장소를 정리하도록 안내한다.
    @Transactional
    fun withdraw(userId: Long) {
        val adminMappings = userPlaceMappingRepository.findAllByUserIdAndAccessLevel(userId, AccessLevel.ADMIN)
        val soleAdminPlaceNames = adminMappings
            .filter { mapping ->
                val otherAdmins = userPlaceMappingRepository
                    .findAllByPlaceIdAndAccessLevel(mapping.place.id, AccessLevel.ADMIN)
                otherAdmins.none { it.user.id != userId }
            }
            .map { it.place.name }

        if (soleAdminPlaceNames.isNotEmpty()) {
            throw BusinessAlertException(
                CommonErrorCode.CONFLICT,
                "${soleAdminPlaceNames.joinToString(", ")} 장소의 유일한 관리자입니다. " +
                    "다른 관리자를 먼저 초대하거나 장소를 삭제한 뒤 탈퇴할 수 있습니다.",
            )
        }

        userPlaceMappingRepository.deleteAll(userPlaceMappingRepository.findAllByUserId(userId))
        val user = userRepository.findById(userId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 회원 정보입니다.")
        }
        userRepository.delete(user)
        redisUtil.delete("refresh:$userId")
    }

    private fun login(provider: OAuthProvider, info: SocialUserInfo): AuthDto.LoginResponse {
        val existing = userRepository.findByProviderAndProviderId(provider, info.providerId)

        val (user, isNewUser) = if (existing != null) {
            // 과거(닉네임 동의 검증 도입 전 등) 가입된 계정 중 name이 비어있는 경우를 로그인 시점에
            // 소셜 프로바이더 최신 정보로 채워준다(#177) - 이후 사용자가 직접 수정한 이름은 절대
            // 덮어쓰지 않도록 "비어있을 때만" 채우는 걸로 제한(매 로그인마다 덮어쓰면 프로필 편집이 무의미해짐)
            if (existing.name.isBlank()) {
                existing.updateName(info.name)
            }
            existing to false
        } else {
            if (info.email != null && userRepository.existsByEmail(info.email)) {
                throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 다른 소셜 계정으로 가입된 이메일입니다.")
            }
            val saved = try {
                userRepository.saveAndFlush(
                    User(
                        email = info.email,
                        name = info.name,
                        profileImage = info.profileImage,
                        provider = provider,
                        providerId = info.providerId,
                    ),
                )
            } catch (e: DataIntegrityViolationException) {
                throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 존재하는 계정입니다.")
            }
            saved to true
        }

        val accessToken = jwtProvider.createAccessToken(user.id)
        val refreshToken = jwtProvider.createRefreshToken(user.id)
        persistRefreshTokenAfterCommit(user.id, refreshToken)

        return AuthConverter.toLoginResponse(user, accessToken, refreshToken, isNewUser)
    }

    private fun persistRefreshTokenAfterCommit(userId: Long, refreshToken: String) {
        val expiry = Duration.ofMillis(jwtProvider.refreshTokenExpiry)
        val write = { redisUtil.set("refresh:$userId", refreshToken, expiry) }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = write()
                },
            )
        } else {
            write()
        }
    }

    companion object {
        private val ALLOWED_IMAGE_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_PROFILE_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L
    }
}
