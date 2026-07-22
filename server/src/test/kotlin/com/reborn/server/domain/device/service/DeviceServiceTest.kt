package com.reborn.server.domain.device.service

import com.reborn.server.domain.auth.OAuthProvider
import com.reborn.server.domain.auth.User
import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceSerial
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.dto.DeviceDto
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.device.repository.DeviceSerialRepository
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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
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
class DeviceServiceTest {

    @Mock
    private lateinit var placeRepository: PlaceRepository

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var deviceSerialRepository: DeviceSerialRepository

    @Mock
    private lateinit var userPlaceMappingRepository: UserPlaceMappingRepository

    @Mock
    private lateinit var redisUtil: RedisUtil

    // @Value 문자열 필드(operatorApiKey)가 섞여있어 @InjectMocks 대신 직접 생성한다
    // (SmartThingsServiceTest와 동일 패턴).
    private lateinit var deviceService: DeviceService

    private lateinit var place: Place
    private lateinit var user: User
    private lateinit var adminMapping: UserPlaceMapping

    @BeforeEach
    fun setUp() {
        place = Place(name = "테스트 거실", qrCode = "qr-test", type = PlaceType.HOME, id = 501)
        user = User(email = "test@reborn.com", name = "테스트", provider = OAuthProvider.GOOGLE, providerId = "google-1", id = 1)
        adminMapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.ADMIN)
        deviceService = DeviceService(
            placeRepository = placeRepository,
            deviceRepository = deviceRepository,
            deviceSerialRepository = deviceSerialRepository,
            userPlaceMappingRepository = userPlaceMappingRepository,
            redisUtil = redisUtil,
            operatorApiKey = "test-operator-key",
        )
    }

    @Test
    fun `register - 미할당 시리얼이면 시리얼의 기기 타입으로 등록한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "AI7K2P9M", deviceName = "거실 스피커")
        val serial = DeviceSerial(serial = "AI7K2P9M", deviceType = DeviceType.AI_SPEAKER)
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.AI_SPEAKER,
            deviceKey = "AI7K2P9M",
            name = "거실 스피커",
        ).apply { prePersist() }

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(deviceSerialRepository.findBySerial("AI7K2P9M")).willReturn(serial)
        given(deviceRepository.save(any())).willReturn(savedDevice)

        val response = deviceService.register(1L, request)

        assertThat(response.deviceId).isEqualTo("AI7K2P9M")
        assertThat(response.deviceType).isEqualTo("AI_SPEAKER")
        verify(deviceSerialRepository).save(serial)
        assertThat(serial.assignedDevice).isEqualTo(savedDevice)
    }

    @Test
    fun `register - 존재하지 않는 장소면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "AR7K2P9M", deviceName = "안방")
        given(placeRepository.findById(501L)).willReturn(Optional.empty())

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `register - ADMIN 권한이 없으면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "AR7K2P9M", deviceName = "안방")
        val userMapping = UserPlaceMapping(user = user, place = place, accessLevel = AccessLevel.USER)

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(userMapping)

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `register - 발급되지 않은 시리얼이면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "AR000000", deviceName = "안방")

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(deviceSerialRepository.findBySerial("AR000000")).willReturn(null)

        assertThatThrownBy { deviceService.register(1L, request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `register - 이미 할당된 시리얼이면 예외가 발생한다`() {
        val request = DeviceDto.RegisterRequest(placeId = 501, deviceId = "AR7K2P9M", deviceName = "안방")
        val otherDevice = Device(place = place, deviceType = DeviceType.ARDUINO, deviceKey = "AR7K2P9M", id = 99)
        val serial = DeviceSerial(serial = "AR7K2P9M", deviceType = DeviceType.ARDUINO, assignedDevice = otherDevice)

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(deviceSerialRepository.findBySerial("AR7K2P9M")).willReturn(serial)

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
    fun `generateSerialBatch - 운영자 키가 맞으면 지정 개수만큼 시리얼을 생성한다`() {
        given(deviceSerialRepository.save(any())).willAnswer { it.arguments[0] }

        val response = deviceService.generateSerialBatch("test-operator-key", DeviceType.AI_SPEAKER, 3)

        assertThat(response.serials).hasSize(3)
        response.serials.forEach {
            assertThat(it).hasSize(8)
            assertThat(it).startsWith("AI")
        }
    }

    @Test
    fun `generateSerialBatch - 운영자 키가 다르면 예외가 발생한다`() {
        assertThatThrownBy { deviceService.generateSerialBatch("wrong-key", DeviceType.ARDUINO, 3) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `generateSerialBatch - 시리얼 발급 대상이 아닌 기기 유형이면 예외가 발생한다`() {
        assertThatThrownBy { deviceService.generateSerialBatch("test-operator-key", DeviceType.AEROMETER, 3) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `generateSerialBatch - 개수가 범위를 벗어나면 예외가 발생한다`() {
        assertThatThrownBy { deviceService.generateSerialBatch("test-operator-key", DeviceType.ARDUINO, 0) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `generateSerialBatch - 충돌이 반복되면 예외가 발생한다`() {
        given(deviceSerialRepository.save(any())).willThrow(DataIntegrityViolationException("duplicate"))

        assertThatThrownBy { deviceService.generateSerialBatch("test-operator-key", DeviceType.ARDUINO, 1) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `generateAndRegisterDevice - 운영자 키가 맞으면 시리얼 발급과 동시에 기기를 등록한다`() {
        val savedDevice = Device(
            place = place,
            deviceType = DeviceType.AI_SPEAKER,
            deviceKey = "AI7K2P9M",
            name = "거실 스피커",
        ).apply { prePersist() }

        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(deviceSerialRepository.save(any())).willAnswer { it.arguments[0] }
        given(deviceRepository.save(any())).willReturn(savedDevice)

        val response = deviceService.generateAndRegisterDevice("test-operator-key", DeviceType.AI_SPEAKER, 501L, "거실 스피커")

        assertThat(response.deviceType).isEqualTo("AI_SPEAKER")
        assertThat(response.deviceName).isEqualTo("거실 스피커")
        verify(deviceSerialRepository, Mockito.times(2)).save(any())
    }

    @Test
    fun `generateAndRegisterDevice - 운영자 키가 다르면 예외가 발생한다`() {
        assertThatThrownBy { deviceService.generateAndRegisterDevice("wrong-key", DeviceType.ARDUINO, 501L, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.FORBIDDEN)
    }

    @Test
    fun `generateAndRegisterDevice - 시리얼 발급 대상이 아닌 기기 유형이면 예외가 발생한다`() {
        assertThatThrownBy { deviceService.generateAndRegisterDevice("test-operator-key", DeviceType.SMART_THINGS, 501L, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `generateAndRegisterDevice - 존재하지 않는 장소면 예외가 발생한다`() {
        given(placeRepository.findById(999L)).willReturn(Optional.empty())

        assertThatThrownBy { deviceService.generateAndRegisterDevice("test-operator-key", DeviceType.ARDUINO, 999L, null) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }

    @Test
    fun `generatePairingCode - ADMIN이면 코드를 생성한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(redisUtil.setIfAbsent(anyString(), anyString(), anyDuration())).willReturn(true)

        val response = deviceService.generatePairingCode(1L, 501L)

        assertThat(response.pairingCode).hasSize(6)
        assertThat(response.expiresAt).isAfter(LocalDateTime.now())
        verify(redisUtil).setIfAbsent("pairing:${response.pairingCode}", "501", Duration.ofMinutes(10))
    }

    @Test
    fun `generatePairingCode - 코드 생성이 계속 충돌하면 예외가 발생한다`() {
        given(placeRepository.existsById(501L)).willReturn(true)
        given(userPlaceMappingRepository.findByUserIdAndPlaceId(1L, 501L)).willReturn(adminMapping)
        given(redisUtil.setIfAbsent(anyString(), anyString(), anyDuration())).willReturn(false)

        assertThatThrownBy { deviceService.generatePairingCode(1L, 501L) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR)
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

        given(redisUtil.getAndDelete("pairing:ABC123")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(deviceRepository.save(any())).willReturn(savedDevice)

        val response = deviceService.pairDevice(request)

        assertThat(response.deviceId).isEqualTo("device-uuid")
        assertThat(response.placeId).isEqualTo(501L)
        assertThat(response.appToken).isNotBlank()
        verify(redisUtil).getAndDelete("pairing:ABC123")
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
        given(redisUtil.getAndDelete("pairing:INVALID")).willReturn(null)

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `pairDevice - 저장 중 충돌이 발생하면 예외가 발생한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = "ABC123", deviceName = "거실 공기계")

        given(redisUtil.getAndDelete("pairing:ABC123")).willReturn("501")
        given(placeRepository.findById(501L)).willReturn(Optional.of(place))
        given(deviceRepository.save(any())).willThrow(DataIntegrityViolationException("duplicate"))

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.CONFLICT)
    }

    @Test
    fun `pairDevice - 기기 이름이 없으면 예외가 발생한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = "ABC123", deviceName = " ")

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `pairDevice - 장소를 찾을 수 없으면 예외가 발생한다`() {
        val request = DeviceDto.PairingRequest(pairingCode = "ABC123", deviceName = "거실 공기계")

        given(redisUtil.getAndDelete("pairing:ABC123")).willReturn("999")
        given(placeRepository.findById(999L)).willReturn(Optional.empty())

        assertThatThrownBy { deviceService.pairDevice(request) }
            .isInstanceOf(BusinessAlertException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.NOT_FOUND)
    }
}
