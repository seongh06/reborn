package com.reborn.server.domain.smartthings.client

import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

// SmartThings 기기 조회/제어 클라이언트(#132). accessToken은 place별로 달라지므로(SmartThingsAuthClient의
// client_id/secret과 달리) 매 호출마다 인자로 받는다 — 어떤 place의 토큰인지는 호출부(SmartThingsDeviceService)가 결정.
@Component
class SmartThingsDeviceClient(
    restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${smartthings.api-base-url:https://api.smartthings.com/v1}") private val baseUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .build()

    fun getDevices(accessToken: String): List<SmartThingsDeviceSummary> {
        val headers = HttpHeaders().apply { setBearerAuth(accessToken) }

        val response = runCatching {
            restTemplate.exchange(
                "$baseUrl/devices",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                SmartThingsDeviceListApiResponse::class.java,
            ).body
        }.onFailure { e -> log.warn("SmartThings 기기 목록 조회 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "SmartThings 기기 목록 조회에 실패했습니다.")

        return response.items.map { SmartThingsDeviceSummary(deviceId = it.deviceId, label = it.label ?: it.name) }
    }

    fun sendCommands(accessToken: String, deviceId: String, commands: List<SmartThingsCommand>) {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
            contentType = MediaType.APPLICATION_JSON
        }
        val body = SmartThingsCommandRequest(commands)

        runCatching {
            restTemplate.exchange(
                "$baseUrl/devices/$deviceId/commands",
                HttpMethod.POST,
                HttpEntity(body, headers),
                String::class.java,
            )
        }.onFailure { e ->
            log.warn("SmartThings 기기 제어 실패: deviceId={}, error={}", deviceId, e.message)
            throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "SmartThings 기기 제어에 실패했습니다.")
        }
    }

    private data class SmartThingsDeviceListApiResponse(val items: List<SmartThingsDeviceApiItem> = emptyList())

    private data class SmartThingsDeviceApiItem(
        val deviceId: String,
        val label: String? = null,
        val name: String? = null,
    )
}

data class SmartThingsDeviceSummary(
    val deviceId: String,
    val label: String?,
)

data class SmartThingsCommand(
    val component: String = "main",
    val capability: String,
    val command: String,
    val arguments: List<Any> = emptyList(),
)

private data class SmartThingsCommandRequest(val commands: List<SmartThingsCommand>)
