package com.reborn.server.domain.device.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.AccessLevel
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceRepository
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.domain.place.UserPlaceMapping
import com.reborn.server.domain.place.UserPlaceMappingRepository
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.redis.RedisUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DeviceServiceTest {

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var redisUtil: RedisUtil

    @InjectMocks
    private lateinit var deviceService: DeviceService

    private lateinit var place: Place
    private lateinit var user: User
    private lateinit var adminMapping: UserPlaceMapping

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        adminMapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN)
    }

    @Test
    fun `register - ADMIN이면 기기를 등록한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "arduino_room_02", deviceName = "안방")
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.ARDUINO,
            deviceKey = "arduino_room_02",
            name = "안방",
        ).apply { prePersist() }

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(deviceRepository.existsByDeviceKey("arduino_room_02")).willReturn(false)
        given(deviceRepository.save(any())).willReturn(savedDevice)

        val response = deviceService.register(1L, request)

        assertThat(response.deviceId).isEqualTo("arduino_room_02")
        assertThat(response.deviceName).isEqualTo("안방")
        assertThat(response.deviceType).isEqualTo("ARDUINO")
    }

    @Test
    fun `register - 존재하지 않는 장소면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "arduino_room_02", deviceName = "안방")
        given(placeRepository.findById(501L)).willReturn(Optional.empty())

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `register - ADMIN 권한이 없으면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "arduino_room_02", deviceName = "안방")
        val userMapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.USER)

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(userMapping)

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `register - 이미 등록된 deviceId면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "arduino_room_02", deviceName = "안방")

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(deviceRepository.existsByDeviceKey("arduino_room_02")).willReturn(true)

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.CONFLICT)
    }

    @Test
    fun `register - 필수 필드가 누락되면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = " ", deviceName = "안방")

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `generatePairingCode - ADMIN이면 코드를 생성한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)

        val response = deviceService.generatePairingCode(1L, 501L)

        assertThat(response.pairingCode).hasSize(6)
        assertThat(response.expiresAt).isAfter(LocalDateTime.now())
    }

    @Test
    fun `generatePairingCode - ADMIN 권한이 없으면 예외가 발생한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(null)

        assertThatThrownBy { deviceService.generatePairingCode(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `generatePairingCode - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.existsById(999L)).willReturn(false)

        assertThatThrownBy { deviceService.generatePairingCode(1L, 999L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `pairDevice - 유효한 코드면 AEROMETER 기기를 등록한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = "ABC123", deviceName = "거실 공기계")
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.AEROMETER,
            deviceKey = "device-uuid",
            name = "거실 공기계",
            appToken = "token-uuid",
            id = 10,
        )

        given(redisUtil.get("pairing:ABC123")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(deviceRepository.save(any())).willReturn(savedDevice)

        val response = deviceService.pairDevice(request)

        assertThat(response.deviceId).isEqualTo("device-uuid")
        assertThat(response.placeId).isEqualTo(501L)
        assertThat(response.appToken).isNotBlank()
        verify(redisUtil).delete("pairing:ABC123")
    }

    @Test
    fun `pairDevice - 페어링 코드가 없으면 예외가 발생한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = " ", deviceName = "거실 공기계")

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `pairDevice - 코드가 만료되었거나 유효하지 않으면 예외가 발생한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = "INVALID", deviceName = "거실 공기계")
        given(redisUtil.get("pairing:INVALID")).willReturn(null)

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
