package com.reborn.server.domain.smartthings.client

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

// SmartThings OAuth 2.0 Authorization Code Grant 토큰 교환/리프레시 클라이언트(#130).
// SmartThings는 PKCE(공개 클라이언트)를 지원하지 않아 client_secret이 필요하므로,
// 이 클라이언트는 서버에서만 사용한다 — 모바일 앱(공기계/관리자)은 직접 호출하지 않는다.
@Component
class SmartThingsAuthClient(
    restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${smartthings.client-id:}") private val clientId: String,
    @param:Value("\${smartthings.client-secret:}") private val clientSecret: String,
    @param:Value("\${smartthings.redirect-uri:}") private val redirectUri: String,
    @param:Value("\${smartthings.token-url:https://api.smartthings.com/oauth/token}") private val tokenUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .build()

    fun exchangeCode(code: String): SmartThingsTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
        }
        return requestToken(body, "authorization_code 교환")
    }

    fun refresh(refreshToken: String): SmartThingsTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
        }
        return requestToken(body, "refresh_token 갱신")
    }

    private fun requestToken(body: LinkedMultiValueMap<String, String>, context: String): SmartThingsTokenResponse {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "SmartThings OAuth 앱이 설정되지 않았습니다.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            setBasicAuth(clientId, clientSecret)
        }

        val response = runCatching {
            restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                HttpEntity(body, headers),
                SmartThingsTokenResponse::class.java,
            ).body
        }.onFailure { e -> log.warn("SmartThings {} 실패: {}", context, e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "SmartThings 인증에 실패했습니다.")

        return response
    }
}

data class SmartThingsTokenResponse(
    @param:JsonProperty("access_token") val accessToken: String,
    @param:JsonProperty("refresh_token") val refreshToken: String,
    @param:JsonProperty("expires_in") val expiresInSeconds: Long,
)
