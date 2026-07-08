package com.reborn.server.domain.place.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.domain.place.dto.PlaceDto
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PlaceServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var redisUtil: RedisUtil

    @InjectMocks
    private lateinit var placeService: PlaceService

    private lateinit var user: User
    private lateinit var place: Place

    @BeforeEach
    fun setUp() {
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        place = Place(name = "우리집", qrCode = "qr-uuid", type = PlaceType.HOME, id = 501)
    }

    @Test
    fun `register - 정상 요청이면 장소를 등록하고 ADMIN 권한을 부여한다`() {
        val request = PlaceDto.RegisterRequest(name = "우리집", type = "HOME")
        val savedPlace = Place(name = "우리집", qrCode = "qr-uuid", type = PlaceType.HOME, id = 501).apply { prePersist() }

        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(placeRepository.save(any())).willReturn(savedPlace)

        val response = placeService.register(1L, request)

        assertThat(response.placeId).isEqualTo(501L)
        assertThat(response.name).isEqualTo("우리집")
        assertThat(response.type).isEqualTo("HOME")
        assertThat(response.createdAt).isEqualTo(savedPlace.createdAt)

        val mappingCaptor = ArgumentCaptor.forClass(UserPlaceMapping::class.java)
        verify(userPlaceMappingRepository).save(mappingCaptor.capture())
        assertThat(mappingCaptor.value.accessLevel).isEqualTo(AccessLevel.ADMIN)
        assertThat(mappingCaptor.value.user).isEqualTo(user)
        assertThat(mappingCaptor.value.place).isEqualTo(savedPlace)
    }

    @Test
    fun `register - 장소 이름이 없으면 예외가 발생한다`() {
        val request = PlaceDto.RegisterRequest(name = " ", type = "HOME")

        assertThatThrownBy { placeService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `register - 정의되지 않은 공간 유형이면 예외가 발생한다`() {
        val request = PlaceDto.RegisterRequest(name = "우리집", type = "UNKNOWN")

        assertThatThrownBy { placeService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `register - 토큰의 회원이 존재하지 않으면 예외가 발생한다`() {
        val request = PlaceDto.RegisterRequest(name = "우리집", type = "HOME")
        given(userRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy { placeService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `generateAdminCode - ADMIN이면 코드를 생성한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L))
            .willReturn(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN))

        val response = placeService.generateAdminCode(1L, 501L)

        assertThat(response.adminCode).hasSize(8)
        assertThat(response.expiresAt).isAfter(LocalDateTime.now())
    }

    @Test
    fun `redeemAdminCode - 유효한 코드면 ADMIN 권한을 부여한다`() {
        val request = PlaceDto.AdminInviteRequest(adminCode = "ABCD1234")

        given(redisUtil.get("admin-invite:ABCD1234")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, 501L)).willReturn(false)

        val response = placeService.redeemAdminCode(1L, request)

        assertThat(response.placeId).isEqualTo(501L)
        assertThat(response.placeName).isEqualTo("우리집")
        assertThat(response.accessLevel).isEqualTo("ADMIN")
        verify(redisUtil).delete("admin-invite:ABCD1234")
    }

    @Test
    fun `redeemAdminCode - 이미 해당 장소에 등록된 사용자면 예외가 발생한다`() {
        val request = PlaceDto.AdminInviteRequest(adminCode = "ABCD1234")

        given(redisUtil.get("admin-invite:ABCD1234")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userRepository.findById(1L)).willReturn(Optional.of(user))
        given(userPlaceMappingRepository.existsByUserIdAndPlaceId(1L, 501L)).willReturn(true)

        assertThatThrownBy { placeService.redeemAdminCode(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.CONFLICT)
    }

    @Test
    fun `redeemAdminCode - 코드가 만료되었거나 유효하지 않으면 예외가 발생한다`() {
        val request = PlaceDto.AdminInviteRequest(adminCode = "INVALID1")
        given(redisUtil.get("admin-invite:INVALID1")).willReturn(null)

        assertThatThrownBy { placeService.redeemAdminCode(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
