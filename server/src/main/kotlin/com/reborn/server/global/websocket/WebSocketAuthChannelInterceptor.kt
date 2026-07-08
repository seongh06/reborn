package com.reborn.server.global.websocket

import com.reborn.server.domain.device.DeviceType
import com.reborn.server.domain.device.repository.DeviceRepository
import com.reborn.server.global.token.JwtProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class WebSocketAuthChannelInterceptor(
    private val jwtProvider: JwtProvider,
    private val deviceRepository: DeviceRepository,
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.CONNECT) {
            return message
        }
        accessor.user = authenticate(accessor)
        return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
    }

    private fun authenticate(accessor: StompHeaderAccessor): StompPrincipal {
        val authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER)
        val deviceKey = accessor.getFirstNativeHeader(DEVICE_KEY_HEADER)

        return when {
            !authHeader.isNullOrBlank() -> authenticateAdmin(authHeader)
            !deviceKey.isNullOrBlank() -> authenticateAerometer(deviceKey, accessor.getFirstNativeHeader(APP_TOKEN_HEADER))
            else -> throw IllegalArgumentException("인증 정보가 없습니다. Authorization 또는 X-Device-Key 헤더가 필요합니다.")
        }
    }

    private fun authenticateAdmin(authHeader: String): StompPrincipal {
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw IllegalArgumentException("Authorization 헤더 형식이 올바르지 않습니다.")
        }
        val token = authHeader.removePrefix(BEARER_PREFIX)
        val claims = jwtProvider.parseClaims(token)
            ?.takeIf { it[JwtProvider.TYPE_KEY] == JwtProvider.ACCESS_TYPE }
            ?: throw IllegalArgumentException("유효하지 않거나 만료된 AccessToken입니다.")
        val userId = claims.subject?.toLongOrNull()
            ?: throw IllegalArgumentException("유효하지 않은 AccessToken입니다.")

        return StompPrincipal(name = userId.toString(), role = ConnectionRole.ADMIN)
    }

    private fun authenticateAerometer(deviceKey: String, appToken: String?): StompPrincipal {
        val device = deviceRepository.findByDeviceKey(deviceKey)
            ?.takeIf { it.deviceType == DeviceType.AEROMETER }
            ?: throw IllegalArgumentException("등록되지 않은 공기계 기기입니다.")

        if (appToken.isNullOrBlank() || device.appToken != appToken) {
            throw IllegalArgumentException("유효하지 않은 앱 토큰입니다.")
        }

        return StompPrincipal(name = deviceKey, role = ConnectionRole.AEROMETER)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val DEVICE_KEY_HEADER = "X-Device-Key"
        private const val APP_TOKEN_HEADER = "X-App-Token"
    }
}
