package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.SmartThingsCredential
import com.reborn.server.domain.smartthings.SmartThingsCredentialRepository
import com.reborn.server.domain.smartthings.client.SmartThingsAuthClient
import com.reborn.server.domain.smartthings.dto.SmartThingsDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.util.generateUuid
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.LocalDateTime

// SmartThings OAuth 연동(#130) — 장소별 accessToken/refreshToken을 서버가 직접 보유하고 관리한다.
// SmartThings는 PKCE(공개 클라이언트)를 지원하지 않아 client_secret이 서버에만 있어야 하므로,
// 공기계/관리자 앱은 이 토큰을 직접 다루지 않는다(CLAUDE.md "실시간 IoT 제어 흐름" 참고).
@Service
@Transactional(readOnly = true)
class SmartThingsService(
    private val placeRepository: PlaceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val smartThingsCredentialRepository: SmartThingsCredentialRepository,
    private val smartThingsAuthClient: SmartThingsAuthClient,
    private val redisUtil: RedisUtil,
    @param:Value("\${smartthings.client-id:}") private val clientId: String,
    @param:Value("\${smartthings.redirect-uri:}") private val redirectUri: String,
    @param:Value("\${smartthings.authorize-url:https://api.smartthings.com/oauth/authorize}") private val authorizeUrl: String,
    @param:Value("\${smartthings.scope:r:devices:* x:devices:*}") private val scope: String,
) {

    fun startAuthorize(userId: Long, placeId: Long): SmartThingsDto.AuthorizeResponse {
        if (!placeRepository.existsById(placeId)) {
            throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }
        requireAdmin(userId, placeId)

        val state = generateUuid()
        redisUtil.set("$STATE_PREFIX$state", placeId.toString(), Duration.ofMinutes(STATE_TTL_MINUTES))

        val url = UriComponentsBuilder.fromUriString(authorizeUrl)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scope)
            .queryParam("state", state)
            .build()
            .toUriString()

        return SmartThingsDto.AuthorizeResponse(authorizeUrl = url)
    }

    @Transactional
    fun handleCallback(code: String?, state: String?): String {
        val safeCode = code?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "인가 코드가 없습니다.")
        val safeState = state?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "state가 없습니다.")

        val placeId = redisUtil.getAndDelete("$STATE_PREFIX$safeState")?.toLongOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "만료되었거나 유효하지 않은 연동 요청입니다.")

        val place = placeRepository.findById(placeId).orElseThrow {
            BusinessAlertException(CommonErrorCode.NOT_FOUND, "존재하지 않는 장소 정보입니다.")
        }

        val token = smartThingsAuthClient.exchangeCode(safeCode)
        val expiresAt = LocalDateTime.now().plusSeconds(token.expiresInSeconds)

        val existing = smartThingsCredentialRepository.findByPlaceId(placeId)
        if (existing != null) {
            existing.updateTokens(token.accessToken, token.refreshToken, expiresAt)
        } else {
            smartThingsCredentialRepository.save(
                SmartThingsCredential(
                    place = place,
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAt = expiresAt,
                ),
            )
        }

        return place.name
    }

    // #132/#133(기기 제어·온습도 폴링)에서 재사용할 진입점 — 만료 임박 시 자동 갱신 후 유효한 accessToken을 반환한다.
    @Transactional
    fun getValidAccessToken(placeId: Long): String {
        val credential = smartThingsCredentialRepository.findByPlaceId(placeId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "이 장소는 SmartThings와 연동되어 있지 않습니다.")

        if (credential.expiresAt.isAfter(LocalDateTime.now().plusSeconds(EXPIRY_BUFFER_SECONDS))) {
            return credential.accessToken
        }

        val refreshed = smartThingsAuthClient.refresh(credential.refreshToken)
        val expiresAt = LocalDateTime.now().plusSeconds(refreshed.expiresInSeconds)
        credential.updateTokens(refreshed.accessToken, refreshed.refreshToken, expiresAt)
        return credential.accessToken
    }

    private fun requireAdmin(userId: Long, placeId: Long) {
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }

    companion object {
        private const val STATE_PREFIX = "smartthings:oauth:state:"
        private const val STATE_TTL_MINUTES = 10L
        private const val EXPIRY_BUFFER_SECONDS = 60L
    }
}
