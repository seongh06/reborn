package com.reborn.server.domain.auth.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.auth.client.GoogleAuthClient
import com.reborn.server.domain.auth.client.KakaoAuthClient
import com.reborn.server.domain.auth.client.SocialUserInfo
import com.reborn.server.domain.auth.converter.AuthConverter
import com.reborn.server.domain.auth.dto.AuthDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.token.JwtProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val kakaoAuthClient: KakaoAuthClient,
    private val jwtProvider: JwtProvider,
    private val redisUtil: RedisUtil,
) {

    @Transactional
    fun loginWithGoogle(request: AuthDto.GoogleLoginRequest): AuthDto.LoginResponse {
        val idToken = request.idToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "idToken은 필수입니다.")
        return login(OAuthProvider.GOOGLE, googleAuthClient.verify(idToken))
    }

    @Transactional
    fun loginWithKakao(request: AuthDto.KakaoLoginRequest): AuthDto.LoginResponse {
        val accessToken = request.accessToken?.takeIf { it.isNotBlank() }
            ?: throw BusinessAlertException(CommonErrorCode.INVALID_INPUT, "accessToken은 필수입니다.")
        return login(OAuthProvider.KAKAO, kakaoAuthClient.verify(accessToken))
    }

    private fun login(provider: OAuthProvider, info: SocialUserInfo): AuthDto.LoginResponse {
        val existing = userRepository.findByProviderAndProviderId(provider, info.providerId)

        val (user, isNewUser) = if (existing != null) {
            existing to false
        } else {
            if (userRepository.existsByEmail(info.email)) {
                throw BusinessAlertException(CommonErrorCode.CONFLICT, "이미 다른 소셜 계정으로 가입된 이메일입니다.")
            }
            val saved = userRepository.save(
                User(
                    email = info.email,
                    name = info.name,
                    profileImage = info.profileImage,
                    provider = provider,
                    providerId = info.providerId,
                ),
            )
            saved to true
        }

        val accessToken = jwtProvider.createAccessToken(user.id)
        val refreshToken = jwtProvider.createRefreshToken(user.id)
        redisUtil.set("refresh:${user.id}", refreshToken, Duration.ofMillis(jwtProvider.refreshTokenExpiry))

        return AuthConverter.toLoginResponse(user, accessToken, refreshToken, isNewUser)
    }
}
