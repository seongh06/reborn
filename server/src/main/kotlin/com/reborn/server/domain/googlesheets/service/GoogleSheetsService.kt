package com.reborn.server.domain.googlesheets.service

import com.reborn.server.domain.googlesheets.GoogleSheetsCredential
import com.reborn.server.domain.googlesheets.GoogleSheetsCredentialRepository
import com.reborn.server.domain.googlesheets.client.GoogleSheetsAuthClient
import com.reborn.server.domain.googlesheets.dto.GoogleSheetsDto
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.UserPlaceMappingRepository
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

// Google Sheets OAuth 연동(데이터 화면 내보내기) - SmartThingsService(#130)와 동일 패턴으로
// 장소별 accessToken/refreshToken을 서버가 직접 보유한다.
@Service
@Transactional(readOnly = true)
class GoogleSheetsService(
    private val placeRepository: PlaceRepository,
    private val userPlaceMappingRepository: UserPlaceMappingRepository,
    private val googleSheetsCredentialRepository: GoogleSheetsCredentialRepository,
    private val googleSheetsAuthClient: GoogleSheetsAuthClient,
    private val redisUtil: RedisUtil,
    @param:Value("\${google-sheets.client-id:}") private val clientId: String,
    @param:Value("\${google-sheets.redirect-uri:}") private val redirectUri: String,
    @param:Value("\${google-sheets.authorize-url:https://accounts.google.com/o/oauth2/v2/auth}") private val authorizeUrl: String,
    @param:Value("\${google-sheets.scope:https://www.googleapis.com/auth/spreadsheets}") private val scope: String,
) {

    fun startAuthorize(userId: Long, placeId: Long): GoogleSheetsDto.AuthorizeResponse {
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
            // refresh_token을 매번 확실히 받기 위해 offline 접근 + 항상 동의 화면 표시
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .queryParam("state", state)
            .build()
            .toUriString()

        return GoogleSheetsDto.AuthorizeResponse(authorizeUrl = url)
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

        val token = googleSheetsAuthClient.exchangeCode(safeCode)
        val expiresAt = LocalDateTime.now().plusSeconds(token.expiresInSeconds)
        val existing = googleSheetsCredentialRepository.findByPlaceId(placeId)

        // Google은 재동의(prompt=consent)가 아니면 refresh_token을 다시 안 내려줄 수 있음 - 최초 연동이
        // 아닌데 refresh_token이 없으면 기존 값을 유지, 최초 연동인데 없으면 재시도를 안내한다.
        val refreshToken = token.refreshToken ?: existing?.refreshToken
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google 인증에 실패했습니다. 다시 연동해주세요.")

        if (existing != null) {
            existing.updateTokens(token.accessToken, refreshToken, expiresAt)
        } else {
            googleSheetsCredentialRepository.save(
                GoogleSheetsCredential(
                    place = place,
                    accessToken = token.accessToken,
                    refreshToken = refreshToken,
                    expiresAt = expiresAt,
                ),
            )
        }

        return place.name
    }

    // 데이터 화면 내보내기(MetricExportService)에서 재사용할 진입점.
    @Transactional
    fun getValidAccessToken(placeId: Long): String {
        val credential = googleSheetsCredentialRepository.findByPlaceId(placeId)
            ?: throw BusinessAlertException(CommonErrorCode.NOT_FOUND, "이 장소는 Google Sheets와 연동되어 있지 않습니다.")

        if (credential.expiresAt.isAfter(LocalDateTime.now().plusSeconds(EXPIRY_BUFFER_SECONDS))) {
            return credential.accessToken
        }

        val refreshed = googleSheetsAuthClient.refresh(credential.refreshToken)
        val expiresAt = LocalDateTime.now().plusSeconds(refreshed.expiresInSeconds)
        val refreshToken = refreshed.refreshToken ?: credential.refreshToken
        credential.updateTokens(refreshed.accessToken, refreshToken, expiresAt)
        return credential.accessToken
    }

    private fun requireAdmin(userId: Long, placeId: Long) {
        val accessLevel = userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(userId, placeId)
        if (accessLevel != AccessLevel.ADMIN) {
            throw BusinessAlertException(CommonErrorCode.FORBIDDEN, "ADMIN 권한이 없습니다.")
        }
    }

    companion object {
        private const val STATE_PREFIX = "googlesheets:oauth:state:"
        private const val STATE_TTL_MINUTES = 10L
        private const val EXPIRY_BUFFER_SECONDS = 60L
    }
}
