package com.reborn.server.domain.smartthings.service

import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.smartthings.SmartThingsCredential
import com.reborn.server.domain.smartthings.SmartThingsCredentialRepository
import com.reborn.server.domain.smartthings.client.SmartThingsAuthClient
import com.reborn.server.domain.smartthings.client.SmartThingsTokenResponse
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

// Mockito의 any()/eq()는 Duration 같은 Kotlin non-null 참조 타입 인자에서 null을 반환해
// "must not be null" NPE를 유발한다(DeviceServiceTest와 동일 이슈).
private fun anyDuration(): Duration {
    Mockito.any(Duration::class.java)
    return Duration.ZERO
}

private fun eqString(value: String): String {
    Mockito.eq(value)
    return value
}

@ExtendWith(MockitoExtension::class)
class SmartThingsServiceTest {

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var smartThingsCredentialRepository: SmartThingsCredentialRepository

    @Mock
    private lateinit var smartThingsAuthClient: SmartThingsAuthClient

    @Mock
    private lateinit var redisUtil: RedisUtil

    // @Value 문자열 필드가 섞여있어 @InjectMocks 대신 직접 생성한다(DeviceServiceTest와 다른 이유).
    private lateinit var smartThingsService: SmartThingsService

    private lateinit var place: Place

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        smartThingsService = SmartThingsService(
            placeRepository = placeRepository,
            userPlaceMappingRepository = userPlaceMappingRepository,
            smartThingsCredentialRepository = smartThingsCredentialRepository,
            smartThingsAuthClient = smartThingsAuthClient,
            redisUtil = redisUtil,
            clientId = "test-client-id",
            redirectUri = "https://www.reborn-energy.com/api/smartthings/oauth/callback",
            authorizeUrl = "https://api.smartthings.com/oauth/authorize",
            scope = "r:devices:* x:devices:*",
        )
    }

    @Test
    fun `startAuthorize - ADMIN이면 authorizeUrl을 발급한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.ADMIN)

        val response = smartThingsService.startAuthorize(1L, 501L)

        assertThat(response.authorizeUrl).startsWith("https://api.smartthings.com/oauth/authorize")
        assertThat(response.authorizeUrl).contains("client_id=test-client-id")
        verify(redisUtil).set(anyString(), eqString("501"), anyDuration())
    }

    @Test
    fun `startAuthorize - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findAccessLevelByUserIdAndPlaceId(1L, 501L)).willReturn(AccessLevel.USER)

        assertThatThrownBy { smartThingsService.startAuthorize(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `startAuthorize - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.existsById(999L)).willReturn(false)

        assertThatThrownBy { smartThingsService.startAuthorize(1L, 999L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `handleCallback - 유효한 code와 state면 토큰을 저장한다`() {
        given(redisUtil.getAndDelete("smartthings:oauth:state:state-abc")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(smartThingsAuthClient.exchangeCode("code-abc"))
            .willReturn(SmartThingsTokenResponse(accessToken = "access", refreshToken = "refresh", expiresInSeconds = 3600))
        given(smartThingsCredentialRepository.findByPlaceId(501L)).willReturn(null)

        val placeName = smartThingsService.handleCallback("code-abc", "state-abc")

        assertThat(placeName).isEqualTo("테스트 거실")
        verify(smartThingsCredentialRepository).save(Mockito.any(SmartThingsCredential::class.java))
    }

    @Test
    fun `handleCallback - 기존 연동이 있으면 토큰을 갱신한다`() {
        val existing = SmartThingsCredential(
            place = place,
            accessToken = "old-access",
            refreshToken = "old-refresh",
            expiresAt = LocalDateTime.now(),
        )
        given(redisUtil.getAndDelete("smartthings:oauth:state:state-abc")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(smartThingsAuthClient.exchangeCode("code-abc"))
            .willReturn(SmartThingsTokenResponse(accessToken = "new-access", refreshToken = "new-refresh", expiresInSeconds = 3600))
        given(smartThingsCredentialRepository.findByPlaceId(501L)).willReturn(existing)

        smartThingsService.handleCallback("code-abc", "state-abc")

        assertThat(existing.accessToken).isEqualTo("new-access")
        assertThat(existing.refreshToken).isEqualTo("new-refresh")
        verify(smartThingsCredentialRepository, Mockito.never()).save(Mockito.any(SmartThingsCredential::class.java))
    }

    @Test
    fun `handleCallback - state가 만료되었거나 유효하지 않으면 예외가 발생한다`() {
        given(redisUtil.getAndDelete("smartthings:oauth:state:invalid")).willReturn(null)

        assertThatThrownBy { smartThingsService.handleCallback("code-abc", "invalid") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `handleCallback - code가 없으면 예외가 발생한다`() {
        assertThatThrownBy { smartThingsService.handleCallback(null, "state-abc") }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `getValidAccessToken - 만료 임박이 아니면 기존 토큰을 반환한다`() {
        val credential = SmartThingsCredential(
            place = place,
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = LocalDateTime.now().plusHours(1),
        )
        given(smartThingsCredentialRepository.findByPlaceId(501L)).willReturn(credential)

        val token = smartThingsService.getValidAccessToken(501L)

        assertThat(token).isEqualTo("access")
        Mockito.verifyNoInteractions(smartThingsAuthClient)
    }

    @Test
    fun `getValidAccessToken - 만료 임박이면 refresh 후 새 토큰을 반환한다`() {
        val credential = SmartThingsCredential(
            place = place,
            accessToken = "old-access",
            refreshToken = "old-refresh",
            expiresAt = LocalDateTime.now().plusSeconds(10),
        )
        given(smartThingsCredentialRepository.findByPlaceId(501L)).willReturn(credential)
        given(smartThingsAuthClient.refresh("old-refresh"))
            .willReturn(SmartThingsTokenResponse(accessToken = "new-access", refreshToken = "new-refresh", expiresInSeconds = 3600))

        val token = smartThingsService.getValidAccessToken(501L)

        assertThat(token).isEqualTo("new-access")
        assertThat(credential.refreshToken).isEqualTo("new-refresh")
    }

    @Test
    fun `getValidAccessToken - 연동된 적 없으면 예외가 발생한다`() {
        given(smartThingsCredentialRepository.findByPlaceId(999L)).willReturn(null)

        assertThatThrownBy { smartThingsService.getValidAccessToken(999L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
