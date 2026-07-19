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

    // Arduino/공기계가 없는 장소의 온습도 대체 소스(#133). 기기가 해당 capability를
    // 지원하지 않으면 값이 없을 뿐 에러는 아니다 — 호출부에서 null 여부로 스킵 판단.
    fun getDeviceStatus(accessToken: String, deviceId: String): SmartThingsDeviceStatus {
        val headers = HttpHeaders().apply { setBearerAuth(accessToken) }

        val response = runCatching {
            restTemplate.exchange(
                "$baseUrl/devices/$deviceId/status",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                SmartThingsStatusApiResponse::class.java,
            ).body
        }.onFailure { e -> log.warn("SmartThings 기기 상태 조회 실패: deviceId={}, error={}", deviceId, e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "SmartThings 기기 상태 조회에 실패했습니다.")

        val main = response.components["main"]
        val temperature = main?.temperatureMeasurement?.temperature
        val humidity = main?.relativeHumidityMeasurement?.humidity

        return SmartThingsDeviceStatus(
            temperature = toCelsius(temperature?.value, temperature?.unit),
            humidity = humidity?.value,
        )
    }

    // SmartThings는 기기 로케일/설정에 따라 화씨로 값을 줄 수 있다 — 실기기로 unit 값을
    // 확인해 정확히 검증 필요(#133 이슈에 명시된 미확정 사항). 우선 F만 방어적으로 변환.
    private fun toCelsius(value: Double?, unit: String?): Double? {
        if (value == null) return null
        return if (unit.equals("F", ignoreCase = true)) (value - 32) / 1.8 else value
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

    private data class SmartThingsStatusApiResponse(val components: Map<String, SmartThingsComponentStatus> = emptyMap())

    private data class SmartThingsComponentStatus(
        val temperatureMeasurement: SmartThingsTemperatureMeasurement? = null,
        val relativeHumidityMeasurement: SmartThingsHumidityMeasurement? = null,
    )

    private data class SmartThingsTemperatureMeasurement(val temperature: SmartThingsValueHolder? = null)

    private data class SmartThingsHumidityMeasurement(val humidity: SmartThingsValueHolder? = null)

    private data class SmartThingsValueHolder(val value: Double? = null, val unit: String? = null)
}

data class SmartThingsDeviceSummary(
    val deviceId: String,
    val label: String?,
)

// temperature/humidity 둘 다 null이면 이 기기는 온습도 capability를 지원하지 않는다는 뜻(#133) — 에러 아님.
data class SmartThingsDeviceStatus(
    val temperature: Double?,
    val humidity: Double?,
)

data class SmartThingsCommand(
    val component: String = "main",
    val capability: String,
    val command: String,
    val arguments: List<Any> = emptyList(),
)

private data class SmartThingsCommandRequest(val commands: List<SmartThingsCommand>)
