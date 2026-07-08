package com.reborn.server.global.websocket

import com.reborn.server.domain.device.Device
import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.domain.place.Place
import com.reborn.server.domain.place.PlaceType
import com.reborn.server.global.token.JwtProvider
import io.jsonwebtoken.Claims
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

@ExtendWith(MockitoExtension::class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var deviceRepository: DeviceRepository

    @Mock
    private lateinit var claims: Claims

    @Mock
    private lateinit var channel: MessageChannel

    @InjectMocks
    private lateinit var interceptor: WebSocketAuthChannelInterceptor

    private lateinit var place: Place
    private lateinit var device: Device

    @BeforeEach
    fun setUp() {
        place = Place(name = "우리집", qrCode = "qr-uuid", type = PlaceType.HOME, id = 501)
        device = Device(
            place = place,
            deviceType = DeviceType.AEROMETER,
            deviceKey = "kiosk-1",
            name = "거실 공기계",
            appToken = "app-token-1",
            id = 10,
        )
    }

    private fun connectAccessor(headers: Map<String, String>): StompHeaderAccessor {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        headers.forEach { (key, value) -> accessor.addNativeHeader(key, value) }
        accessor.setLeaveMutable(true)
        return accessor
    }

    @Test
    fun `preSend - 유효한 JWT면 ADMIN으로 인증한다`() {
        val accessor = connectAccessor(mapOf("Authorization" to "Bearer valid-token"))
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

        given(jwtProvider.parseClaims("valid-token")).willReturn(claims)
        given(claims[JwtProvider.TYPE_KEY]).willReturn(JwtProvider.ACCESS_TYPE)
        given(claims.subject).willReturn("1")

        val result = interceptor.preSend(message, channel)

        val principal = StompHeaderAccessor.wrap(result).user as StompPrincipal
        assertThat(principal.name).isEqualTo("1")
        assertThat(principal.role).isEqualTo(ConnectionRole.ADMIN)
    }

    @Test
    fun `preSend - 만료되었거나 변조된 JWT면 예외가 발생한다`() {
        val accessor = connectAccessor(mapOf("Authorization" to "Bearer invalid-token"))
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        given(jwtProvider.parseClaims("invalid-token")).willReturn(null)

        assertThatThrownBy { interceptor.preSend(message, channel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `preSend - deviceKey와 appToken이 일치하면 AEROMETER로 인증한다`() {
        val accessor = connectAccessor(mapOf("X-Device-Key" to "kiosk-1", "X-App-Token" to "app-token-1"))
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        given(deviceRepository.findByDeviceKey("kiosk-1")).willReturn(device)

        val result = interceptor.preSend(message, channel)

        val principal = StompHeaderAccessor.wrap(result).user as StompPrincipal
        assertThat(principal.name).isEqualTo("kiosk-1")
        assertThat(principal.role).isEqualTo(ConnectionRole.AEROMETER)
    }

    @Test
    fun `preSend - appToken이 일치하지 않으면 예외가 발생한다`() {
        val accessor = connectAccessor(mapOf("X-Device-Key" to "kiosk-1", "X-App-Token" to "wrong-token"))
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        given(deviceRepository.findByDeviceKey("kiosk-1")).willReturn(device)

        assertThatThrownBy { interceptor.preSend(message, channel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `preSend - 등록되지 않은 기기면 예외가 발생한다`() {
        val accessor = connectAccessor(mapOf("X-Device-Key" to "unknown", "X-App-Token" to "app-token-1"))
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        given(deviceRepository.findByDeviceKey("unknown")).willReturn(null)

        assertThatThrownBy { interceptor.preSend(message, channel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `preSend - 인증 헤더가 전혀 없으면 예외가 발생한다`() {
        val accessor = connectAccessor(emptyMap())
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

        assertThatThrownBy { interceptor.preSend(message, channel) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
