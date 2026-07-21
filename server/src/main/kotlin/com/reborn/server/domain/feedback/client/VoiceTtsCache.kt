package com.reborn.server.domain.feedback.client

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

// AI 스피커(#142) 응답 안내음은 고정 문구 2개(성공/재시도)뿐이라, 프로세스 생존 기간 동안
// Gemini TTS 결과를 캐싱해 매 요청마다 TTS API를 호출하지 않도록 한다.
@Component
class VoiceTtsCache(
    private val geminiClient: GeminiClient,
) {

    private val cache = ConcurrentHashMap<String, GeminiSpeechResult>()

    fun get(text: String): GeminiSpeechResult =
        cache.computeIfAbsent(text) { geminiClient.synthesizeSpeech(it) }
}
