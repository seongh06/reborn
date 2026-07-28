package com.reborn.server.domain.googlesheets.service

import com.reborn.server.domain.googlesheets.GoogleSheetsCredential
import com.reborn.server.domain.googlesheets.GoogleSheetsCredentialRepository
import com.reborn.server.domain.googlesheets.client.GoogleSheetsAuthClient
import com.reborn.server.domain.googlesheets.client.GoogleSheetsTokenResponse
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional

// SmartThingsServiceTest와 동일 이슈 - Mockito any()/eq()는 Duration 같은 Kotlin non-null
// 참조 타입 인자에서 null을 반환해 NPE를 유발한다.
private fun anyDuration(): Duration {
    Mockito.any(Duration::class.java)
    return Duration.ZERO
}

private fun eqString(value: String): String {
    Mockito.eq(value)
    return value
}

@ExtendWith(MockitoExtension::class)
class GoogleSheetsServiceTest {

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var googleSheetsCredentialRepository: GoogleSheetsCredentialRepository

    @Mock
    private lateinit var googleSheetsAuthClient: GoogleSheetsAuthClient

    @Mock
    private lateinit var redisUtil: RedisUtil

    private lateinit var googleSheetsService: GoogleSheetsService

    private lateinit var place: Place

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        googleSheetsService = GoogleSheetsService(
            placeRepository = placeRepository,
            userPlaceMappingRepository = userPlaceMappingRepository,
            googleSheetsCredentialRepository = googleSheetsCredentialRepository,
            googleSheetsAuthClient = googleSheetsAuthClient,
            redisUtil = redisUtil,
            clientId = "test-client-id",
            redirectUri = "https://www.reborn-energy.com/api/google-sheets/oauth/callback",
            authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth",
            scope = "https://www.googleapis.com/auth/spreadsheets",
        )
    }

    @Test
    fun `startAuthorize - ADMIN이면 authorizeUrl을 발급한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        val response = googleSheetsService.startAuthorize(1L, 501L)

        assertThat(response.authorizeUrl).startsWith("https://accounts.google.com/o/oauth2/v2/auth")
        assertThat(response.authorizeUrl).contains("access_type=offline")
        assertThat(response.authorizeUrl).contains("prompt=consent")
        verify(redisUtil).set(anyString(), eqString("501"), anyDuration())
    }

    @Test
    fun `startAuthorize - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { googleSheetsService.startAuthorize(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `startAuthorize - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.existsById(999L)).willReturn(false)

        assertThatThrownBy { googleSheetsService.startAuthorize(1L, 999L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `handleCallback - 유효한 code와 state면 토큰을 저장한다`() {
        given(redisUtil.getAndDelete("googlesheets:oauth:state:state-abc")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(googleSheetsAuthClient.exchangeCode("code-abc"))
            .willReturn(GoogleSheetsTokenResponse(accessToken = "access", refreshToken = "refresh", expiresInSeconds = 3600))
        given(googleSheetsCredentialRepository.findByPlaceId(501L)).willReturn(null)

        val placeName = googleSheetsService.handleCallback("code-abc", "state-abc")

        assertThat(placeName).isEqualTo("테스트 거실")
        verify(googleSheetsCredentialRepository).save(Mockito.any(GoogleSheetsCredential::class.java))
    }

    @Test
    fun `handleCallback - 기존 연동이 있고 refresh_token이 새로 안 오면 기존 값을 유지한다`() {
        val existing = GoogleSheetsCredential(
            place = place,
            accessToken = "old-access",
            refreshToken = "old-refresh",
            expiresAt = LocalDateTime.now(),
        )
        given(redisUtil.getAndDelete("googlesheets:oauth:state:state-abc")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(googleSheetsAuthClient.exchangeCode("code-abc"))
            .willReturn(GoogleSheetsTokenResponse(accessToken = "new-access", refreshToken = null, expiresInSeconds = 3600))
        given(googleSheetsCredentialRepository.findByPlaceId(501L)).willReturn(existing)

        googleSheetsService.handleCallback("code-abc", "state-abc")

        assertThat(existing.accessToken).isEqualTo("new-access")
        assertThat(existing.refreshToken).isEqualTo("old-refresh")
        verify(googleSheetsCredentialRepository, Mockito.never()).save(Mockito.any(GoogleSheetsCredential::class.java))
    }

    @Test
    fun `handleCallback - 최초 연동인데 refresh_token이 없으면 예외가 발생한다`() {
        given(redisUtil.getAndDelete("googlesheets:oauth:state:state-abc")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(googleSheetsAuthClient.exchangeCode("code-abc"))
            .willReturn(GoogleSheetsTokenResponse(accessToken = "access", refreshToken = null, expiresInSeconds = 3600))
        given(googleSheetsCredentialRepository.findByPlaceId(501L)).willReturn(null)

        assertThatThrownBy { googleSheetsService.handleCallback("code-abc", "state-abc") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `handleCallback - state가 만료되었거나 유효하지 않으면 예외가 발생한다`() {
        given(redisUtil.getAndDelete("googlesheets:oauth:state:invalid")).willReturn(null)

        assertThatThrownBy { googleSheetsService.handleCallback("code-abc", "invalid") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `getValidAccessToken - 만료 임박이 아니면 기존 토큰을 반환한다`() {
        val credential = GoogleSheetsCredential(
            place = place,
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = LocalDateTime.now().plusHours(1),
        )
        given(googleSheetsCredentialRepository.findByPlaceId(501L)).willReturn(credential)

        val token = googleSheetsService.getValidAccessToken(501L)

        assertThat(token).isEqualTo("access")
        Mockito.verifyNoInteractions(googleSheetsAuthClient)
    }

    @Test
    fun `getValidAccessToken - 만료 임박이면 refresh 후 새 토큰을 반환하고 refresh_token은 유지한다`() {
        val credential = GoogleSheetsCredential(
            place = place,
            accessToken = "old-access",
            refreshToken = "old-refresh",
            expiresAt = LocalDateTime.now().plusSeconds(10),
        )
        given(googleSheetsCredentialRepository.findByPlaceId(501L)).willReturn(credential)
        given(googleSheetsAuthClient.refresh("old-refresh"))
            .willReturn(GoogleSheetsTokenResponse(accessToken = "new-access", refreshToken = null, expiresInSeconds = 3600))

        val token = googleSheetsService.getValidAccessToken(501L)

        assertThat(token).isEqualTo("new-access")
        assertThat(credential.refreshToken).isEqualTo("old-refresh")
    }

    @Test
    fun `getValidAccessToken - 연동된 적 없으면 예외가 발생한다`() {
        given(googleSheetsCredentialRepository.findByPlaceId(999L)).willReturn(null)

        assertThatThrownBy { googleSheetsService.getValidAccessToken(999L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
