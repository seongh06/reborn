package com.reborn.server.domain.place.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.UserRepository
import com.reborn.server.domain.device.repository.DeviceRepository
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional

// Mockito의 any()/eq()는 Duration 같은 Kotlin non-null 참조 타입 인자에서 null을 반환해
// "must not be null" NPE를 유발한다. 직접 정의한 매처는 Mockito 스택에 매처를 등록하되
// 실제로는 null이 아닌 값을 반환해 이 문제를 피한다.
private fun anyDuration(): Duration {
    Mockito.any(Duration::class.java)
    return Duration.ZERO
}

@ExtendWith(MockitoExtension::class)
class PlaceServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var redisUtil: RedisUtil

    @InjectMocks
    private lateinit var placeService: PlaceService

    private lateinit var user: User
    private lateinit var place: Place

    @BeforeEach
    fun setUp() {
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        place = Place(name = "우리집", qrCode = "qr-uuid", type = PlaceType.HOME, id = 501).apply { prePersist() }
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
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L))
            .willReturn(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN))
        given(redisUtil.setIfAbsent(anyString(), anyString(), anyDuration())).willReturn(true)

        val response = placeService.generateAdminCode(1L, 501L)

        assertThat(response.adminCode).hasSize(6)
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

    @Test
    fun `getList - 사용자가 속한 장소 목록을 권한과 함께 반환한다`() {
        val mapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN)
        given(userPlaceMappingRepository.findAllByUserId(1L)).willReturn(listOf(mapping))

        val response = placeService.getList(1L)

        assertThat(response.places).hasSize(1)
        assertThat(response.places[0].placeId).isEqualTo(501L)
        assertThat(response.places[0].name).isEqualTo("우리집")
        assertThat(response.places[0].type).isEqualTo("HOME")
        assertThat(response.places[0].accessLevel).isEqualTo("ADMIN")
    }

    @Test
    fun `getDetail - 접근 권한이 있으면 장소 상세 정보를 반환한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L))
            .willReturn(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.USER))
        given(deviceRepository.countByPlaceId(501L)).willReturn(3L)

        val response = placeService.getDetail(1L, 501L)

        assertThat(response.placeId).isEqualTo(501L)
        assertThat(response.accessLevel).isEqualTo("USER")
        assertThat(response.deviceCount).isEqualTo(3)
        assertThat(response.qrCode).isEqualTo("qr-uuid")
    }

    @Test
    fun `getDetail - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.empty())

        assertThatThrownBy { placeService.getDetail(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `getDetail - 접근 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(null)

        assertThatThrownBy { placeService.getDetail(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `deletePlace - ADMIN이면 장소를 삭제한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L))
            .willReturn(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN))

        placeService.deletePlace(1L, 501L)

        verify(placeRepository).deleteByIdInBulk(501L)
    }

    @Test
    fun `deletePlace - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.empty())

        assertThatThrownBy { placeService.deletePlace(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `deletePlace - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L))
            .willReturn(UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.USER))

        assertThatThrownBy { placeService.deletePlace(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }
}
