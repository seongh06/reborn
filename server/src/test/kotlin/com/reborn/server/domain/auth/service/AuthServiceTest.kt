package com.reborn.server.domain.auth.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.auth.client.GoogleAuthClient
import com.reborn.server.domain.auth.client.KakaoAuthClient
import com.reborn.server.domain.auth.client.SocialUserInfo
import com.reborn.server.domain.auth.dto.AuthDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import com.reborn.server.global.token.JwtProvider
import io.jsonwebtoken.Claims
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var googleAuthClient: GoogleAuthClient

    @Mock
    private lateinit var kakaoAuthClient: KakaoAuthClient

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var redisUtil: RedisUtil

    @Mock
    private lateinit var claims: Claims

    @InjectMocks
    private lateinit var authService: AuthService

    @Test
    fun `login - GOOGLE 신규 유저면 회원가입 후 토큰을 발급한다`() {
        val request = AuthDto.LoginRequest(provider = "GOOGLE", token = "id-token")
        val info = SocialUserInfo(providerId = "google-1", email = "new@reborn.com", name = "홍길동", profileImage = null)
        val saved = User(email = "new@reborn.com", name = "홍길동", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)

        given(googleAuthClient.verify("id-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1")).willReturn(null)
        given(userRepository.existsByEmail("new@reborn.com")).willReturn(false)
        given(userRepository.saveAndFlush(any())).willReturn(saved)
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token")
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.isNewUser).isTrue()
        assertThat(response.userId).isEqualTo(1L)
        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
    }

    @Test
    fun `login - GOOGLE 기존 유저면 회원가입 없이 로그인 처리한다`() {
        val request = AuthDto.LoginRequest(provider = "GOOGLE", token = "id-token")
        val info = SocialUserInfo(providerId = "google-1", email = "exist@reborn.com", name = "홍길동", profileImage = null)
        val existing = User(email = "exist@reborn.com", name = "홍길동", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 7)

        given(googleAuthClient.verify("id-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1")).willReturn(existing)
        given(jwtProvider.createAccessToken(7L)).willReturn("access-token")
        given(jwtProvider.createRefreshToken(7L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.isNewUser).isFalse()
        assertThat(response.userId).isEqualTo(7L)
    }

    @Test
    fun `login - 다른 provider가 같은 이메일을 쓰고 있으면 예외가 발생한다`() {
        val request = AuthDto.LoginRequest(provider = "GOOGLE", token = "id-token")
        val info = SocialUserInfo(providerId = "google-1", email = "dup@reborn.com", name = "홍길동", profileImage = null)

        given(googleAuthClient.verify("id-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1")).willReturn(null)
        given(userRepository.existsByEmail("dup@reborn.com")).willReturn(true)

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.CONFLICT)
    }

    @Test
    fun `login - token이 없으면 예외가 발생한다`() {
        val request = AuthDto.LoginRequest(provider = "GOOGLE", token = " ")

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `login - provider 대소문자를 구분하지 않는다`() {
        val request = AuthDto.LoginRequest(provider = "google", token = "id-token")
        val info = SocialUserInfo(providerId = "google-1", email = "new@reborn.com", name = "홍길동", profileImage = null)
        val saved = User(email = "new@reborn.com", name = "홍길동", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)

        given(googleAuthClient.verify("id-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1")).willReturn(null)
        given(userRepository.existsByEmail("new@reborn.com")).willReturn(false)
        given(userRepository.saveAndFlush(any())).willReturn(saved)
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token")
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.userId).isEqualTo(1L)
    }

    @Test
    fun `login - 지원하지 않는 provider면 예외가 발생한다`() {
        val request = AuthDto.LoginRequest(provider = "NAVER", token = "token")

        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `login - KAKAO 신규 유저면 회원가입 후 토큰을 발급한다`() {
        val request = AuthDto.LoginRequest(provider = "KAKAO", token = "kakao-token")
        val info = SocialUserInfo(providerId = "kakao-1", email = "kakao@reborn.com", name = "김철수", profileImage = null)
        val saved = User(email = "kakao@reborn.com", name = "김철수", provider = OAuthProvider.KAKAO, providerId = "kakao-1", id = 2)

        given(kakaoAuthClient.verify("kakao-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.KAKAO, "kakao-1")).willReturn(null)
        given(userRepository.existsByEmail("kakao@reborn.com")).willReturn(false)
        given(userRepository.saveAndFlush(any())).willReturn(saved)
        given(jwtProvider.createAccessToken(2L)).willReturn("access-token")
        given(jwtProvider.createRefreshToken(2L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.isNewUser).isTrue()
        assertThat(response.userId).isEqualTo(2L)
    }

    @Test
    fun `login - KAKAO 이메일 동의가 없어도(email null) 회원가입 후 토큰을 발급한다`() {
        val request = AuthDto.LoginRequest(provider = "KAKAO", token = "kakao-token")
        val info = SocialUserInfo(providerId = "kakao-2", email = null, name = "이영희", profileImage = null)
        val saved = User(email = null, name = "이영희", provider = OAuthProvider.KAKAO, providerId = "kakao-2", id = 3)

        given(kakaoAuthClient.verify("kakao-token")).willReturn(info)
        given(userRepository.findByProviderAndProviderId(OAuthProvider.KAKAO, "kakao-2")).willReturn(null)
        given(userRepository.saveAndFlush(any())).willReturn(saved)
        given(jwtProvider.createAccessToken(3L)).willReturn("access-token")
        given(jwtProvider.createRefreshToken(3L)).willReturn("refresh-token")

        val response = authService.login(request)

        assertThat(response.isNewUser).isTrue()
        assertThat(response.userId).isEqualTo(3L)
        verify(userRepository, never()).existsByEmail(anyString())
    }

    @Test
    fun `refresh - 유효한 refreshToken이면 토큰을 재발급한다`() {
        val request = AuthDto.RefreshRequest(refreshToken = "old-refresh-token")

        given(jwtProvider.parseClaims("old-refresh-token")).willReturn(claims)
        given(claims[JwtProvider.TYPE_KEY]).willReturn(JwtProvider.REFRESH_TYPE)
        given(claims.subject).willReturn("1")
        given(redisUtil.get("refresh:1")).willReturn("old-refresh-token")
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access-token")
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh-token")
        given(jwtProvider.refreshTokenExpiry).willReturn(REFRESH_TOKEN_EXPIRY_MS)

        val response = authService.refresh(request)

        assertThat(response.accessToken).isEqualTo("new-access-token")
        assertThat(response.refreshToken).isEqualTo("new-refresh-token")
        // Duration은 Mockito eq()/any()가 Kotlin non-null 파라미터와 만나면 NPE를 내서(플랫폼 타입이 아닌
        // Kotlin 네이티브 타입 한정 이슈) 매처 대신 실제 값으로 직접 검증한다.
        verify(redisUtil).set("refresh:1", "new-refresh-token", Duration.ofMillis(REFRESH_TOKEN_EXPIRY_MS))
    }

    @Test
    fun `refresh - refreshToken이 없으면 예외가 발생한다`() {
        val request = AuthDto.RefreshRequest(refreshToken = " ")

        assertThatThrownBy { authService.refresh(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `refresh - 만료되었거나 변조된 토큰이면 예외가 발생한다`() {
        val request = AuthDto.RefreshRequest(refreshToken = "invalid-token")
        given(jwtProvider.parseClaims("invalid-token")).willReturn(null)

        assertThatThrownBy { authService.refresh(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.UNAUTHORIZED)
    }

    @Test
    fun `refresh - AccessToken을 refreshToken으로 사용하면 예외가 발생한다`() {
        val request = AuthDto.RefreshRequest(refreshToken = "access-token-used-as-refresh")

        given(jwtProvider.parseClaims("access-token-used-as-refresh")).willReturn(claims)
        given(claims[JwtProvider.TYPE_KEY]).willReturn(JwtProvider.ACCESS_TYPE)

        assertThatThrownBy { authService.refresh(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.UNAUTHORIZED)
    }

    @Test
    fun `refresh - Redis에 저장된 최신 토큰과 다르면 예외가 발생한다`() {
        val request = AuthDto.RefreshRequest(refreshToken = "rotated-out-token")

        given(jwtProvider.parseClaims("rotated-out-token")).willReturn(claims)
        given(claims[JwtProvider.TYPE_KEY]).willReturn(JwtProvider.REFRESH_TYPE)
        given(claims.subject).willReturn("1")
        given(redisUtil.get("refresh:1")).willReturn("newer-token-issued-since")

        assertThatThrownBy { authService.refresh(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.UNAUTHORIZED)
    }

    @Test
    fun `logout - Redis에서 refreshToken을 삭제한다`() {
        authService.logout(1L)

        verify(redisUtil).delete("refresh:1")
    }

    @Test
    fun `updateFcmToken - 정상 요청이면 토큰을 저장한다`() {
        val user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        val request = AuthDto.FcmTokenUpdateRequest(fcmToken = "new-fcm-token")
        given(userRepository.findById(1L)).willReturn(Optional.of(user))

        authService.updateFcmToken(1L, request)

        assertThat(user.fcmToken).isEqualTo("new-fcm-token")
    }

    @Test
    fun `updateFcmToken - fcmToken이 없으면 예외가 발생한다`() {
        val request = AuthDto.FcmTokenUpdateRequest(fcmToken = " ")

        assertThatThrownBy { authService.updateFcmToken(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `updateFcmToken - 존재하지 않는 회원이면 예외가 발생한다`() {
        val request = AuthDto.FcmTokenUpdateRequest(fcmToken = "new-fcm-token")
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy { authService.updateFcmToken(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    companion object {
        private const val REFRESH_TOKEN_EXPIRY_MS = 1209600000L
    }
}
