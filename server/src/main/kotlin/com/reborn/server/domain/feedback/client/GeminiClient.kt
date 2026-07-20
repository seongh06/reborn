package com.reborn.server.domain.feedback.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.Base64

data class GeminiAudioAnalysis(
    val recognized: Boolean,
    val summary: String,
)

data class GeminiSpeechResult(
    val audioBytes: ByteArray,
    val mimeType: String,
)

// Gemini 오디오 이해(음성 피드백 분석) + TTS(응답 음성 합성) 클라이언트(#142, AI 스피커).
// GEMINI_API_KEY 미설정 시 fail-closed. 비용 절감을 위해 기본 모델은 flash-lite 계열을 사용한다.
@Component
class GeminiClient(
    restTemplateBuilder: RestTemplateBuilder,
    private val objectMapper: ObjectMapper,
    @param:Value("\${gemini.api-key:}") private val apiKey: String,
    @param:Value("\${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") private val baseUrl: String,
    @param:Value("\${gemini.model:gemini-2.5-flash-lite}") private val model: String,
    @param:Value("\${gemini.tts-model:gemini-2.5-flash-preview-tts}") private val ttsModel: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .build()

    private val analysisPrompt = """
        다음은 실내 환경 서비스(ReBorn) 이용자가 AI 스피커 버튼을 눌러 남긴 음성 피드백 녹음입니다.
        말한 내용을 알아들을 수 있으면 한국어 한두 문장으로 자연스럽게 요약하고 recognized를 true로 설정하세요.
        잡음뿐이거나, 말이 없거나, 의미를 알아들을 수 없으면 recognized를 false로 설정하고 summary는 빈 문자열로 두세요.
        다른 설명 없이 반드시 다음 JSON 형식으로만 답하세요: {"recognized": boolean, "summary": string}
    """.trimIndent()

    fun analyzeAudio(audioBytes: ByteArray, mimeType: String): GeminiAudioAnalysis {
        requireConfigured()

        val body = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to analysisPrompt),
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to mimeType,
                                "data" to Base64.getEncoder().encodeToString(audioBytes),
                            ),
                        ),
                    ),
                ),
            ),
            "generationConfig" to mapOf("responseMimeType" to "application/json"),
        )

        val response = runCatching { post(model, body) }
            .onFailure { e -> log.warn("Gemini 오디오 분석 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "음성 분석에 실패했습니다.")

        val text = response
            .path("candidates").path(0).path("content").path("parts").path(0).path("text")
            .asText("")
        if (text.isBlank()) {
            log.warn("Gemini 오디오 분석 응답이 비어있음: {}", response)
            return GeminiAudioAnalysis(recognized = false, summary = "")
        }

        val parsed = runCatching { objectMapper.readTree(text) }.getOrNull()
            ?: return GeminiAudioAnalysis(recognized = false, summary = "")

        return GeminiAudioAnalysis(
            recognized = parsed.path("recognized").asBoolean(false),
            summary = parsed.path("summary").asText(""),
        )
    }

    fun synthesizeSpeech(text: String): GeminiSpeechResult {
        requireConfigured()

        val body = mapOf(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to text)))),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("AUDIO"),
                "speechConfig" to mapOf(
                    "voiceConfig" to mapOf(
                        "prebuiltVoiceConfig" to mapOf("voiceName" to "Kore"),
                    ),
                ),
            ),
        )

        val response = runCatching { post(ttsModel, body) }
            .onFailure { e -> log.warn("Gemini TTS 생성 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "TTS 생성에 실패했습니다.")

        val inlineData = response
            .path("candidates").path(0).path("content").path("parts").path(0).path("inlineData")
        val base64Audio = inlineData.path("data").asText("")
        val mimeType = inlineData.path("mimeType").asText("audio/L16;rate=24000")
        if (base64Audio.isBlank()) {
            throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "TTS 응답이 비어있습니다.")
        }

        return GeminiSpeechResult(audioBytes = Base64.getDecoder().decode(base64Audio), mimeType = mimeType)
    }

    private fun post(modelName: String, body: Map<String, Any>): JsonNode {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-goog-api-key", apiKey)
        }
        val url = "$baseUrl/models/$modelName:generateContent"
        val raw = restTemplate.exchange(url, HttpMethod.POST, HttpEntity(body, headers), String::class.java).body
            ?: throw IllegalStateException("Gemini 응답이 비어있습니다.")
        return objectMapper.readTree(raw)
    }

    private fun requireConfigured() {
        if (apiKey.isBlank()) {
            throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Gemini API가 설정되지 않았습니다.")
        }
    }
}
