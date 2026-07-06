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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PlaceServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @InjectMocks
    private lateinit var placeService: PlaceService

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
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
}
