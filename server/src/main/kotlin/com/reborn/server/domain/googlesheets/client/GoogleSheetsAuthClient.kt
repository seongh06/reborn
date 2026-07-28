package com.reborn.server.domain.googlesheets.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Duration

// Google OAuth 2.0 Authorization Code Grant 토큰 교환/리프레시 클라이언트(데이터 화면 Sheets 내보내기).
// SmartThingsAuthClient(#130)와 동일 패턴이나, Google은 refresh_token 요청 시 새 refresh_token을
// 내려주지 않는 경우가 대부분이라 응답 필드를 nullable로 두고 호출부에서 기존 값을 유지한다.
@Component
class GoogleSheetsAuthClient(
    restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${google-sheets.client-id:}") private val clientId: String,
    @param:Value("\${google-sheets.client-secret:}") private val clientSecret: String,
    @param:Value("\${google-sheets.redirect-uri:}") private val redirectUri: String,
    @param:Value("\${google-sheets.token-url:https://oauth2.googleapis.com/token}") private val tokenUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .build()

    fun exchangeCode(code: String): GoogleSheetsTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
        }
        return requestToken(body, "authorization_code 교환")
    }

    fun refresh(refreshToken: String): GoogleSheetsTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
        }
        return requestToken(body, "refresh_token 갱신")
    }

    private fun requestToken(body: LinkedMultiValueMap<String, String>, context: String): GoogleSheetsTokenResponse {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google Sheets OAuth 앱이 설정되지 않았습니다.")
        }

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
        body.add("client_id", clientId)
        body.add("client_secret", clientSecret)

        val response = runCatching {
            restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                HttpEntity(body, headers),
                GoogleSheetsTokenResponse::class.java,
            ).body
        }.onFailure { e -> log.warn("Google Sheets {} 실패: {}", context, e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google 인증에 실패했습니다.")

        return response
    }
}

data class GoogleSheetsTokenResponse(
    @param:JsonProperty("access_token") val accessToken: String,
    // refresh_token 갱신(grant_type=refresh_token) 응답에는 보통 포함되지 않음 - null이면 기존 값 유지
    @param:JsonProperty("refresh_token") val refreshToken: String? = null,
    @param:JsonProperty("expires_in") val expiresInSeconds: Long,
)
