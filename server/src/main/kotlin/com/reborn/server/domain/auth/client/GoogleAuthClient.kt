package com.reborn.server.domain.auth.client

import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class GoogleAuthClient(
    restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${oauth.google.client-id:}") private val clientId: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .build()

    fun verify(idToken: String): SocialUserInfo {
        val response = runCatching {
            restTemplate.getForObject(
                "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}",
                GoogleTokenInfoResponse::class.java,
                idToken,
            )
        }.onFailure { e -> log.debug("Google idToken 검증 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않은 Google ID Token입니다.")

        if (clientId.isNotBlank() && response.aud != clientId) {
            throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않은 Google ID Token입니다.")
        }

        val email = response.email?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않은 Google ID Token입니다.")

        return SocialUserInfo(
            providerId = response.sub,
            email = email,
            name = response.name?.takeIf { it.isNotBlank() } ?: email,
            profileImage = response.picture,
        )
    }

    private data class GoogleTokenInfoResponse(
        val sub: String,
        val aud: String? = null,
        val email: String? = null,
        val name: String? = null,
        val picture: String? = null,
    )
}
