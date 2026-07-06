package com.reborn.server.domain.auth.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class KakaoAuthClient(
    restTemplateBuilder: RestTemplateBuilder,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .build()

    fun verify(accessToken: String): SocialUserInfo {
        val headers = HttpHeaders().apply { setBearerAuth(accessToken) }

        val response = runCatching {
            restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                KakaoUserResponse::class.java,
            ).body
        }.onFailure { e -> log.debug("Kakao accessToken 검증 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "유효하지 않은 Kakao AccessToken입니다.")

        val account = response.kakaoAccount
        val email = account?.email?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "카카오 계정에 이메일 제공 동의가 필요합니다.")

        return SocialUserInfo(
            providerId = response.id.toString(),
            email = email,
            name = account.profile?.nickname?.takeIf { it.isNotBlank() } ?: email,
            profileImage = account.profile?.profileImageUrl,
        )
    }

    private data class KakaoUserResponse(
        val id: Long,
        @param:JsonProperty("kakao_account") val kakaoAccount: KakaoAccount? = null,
    )

    private data class KakaoAccount(
        val email: String? = null,
        val profile: KakaoProfile? = null,
    )

    private data class KakaoProfile(
        val nickname: String? = null,
        @param:JsonProperty("profile_image_url") val profileImageUrl: String? = null,
    )
}
