package com.reborn.core.datastore

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

internal object AuthTokensSerializer : OkioSerializer<AuthTokens> {

    // 필드가 이름 변경/삭제되면(예: tutorialCompleted -> tutorialSeenSteps) 기존 설치 기기에
    // 이미 저장된 예전 스키마 JSON을 디코딩할 때 기본 Json(엄격 모드)은 "unknown key"로 바로
    // 터진다 - 로컬 전용 캐시라 스키마가 앞으로도 계속 바뀔 수 있으니 항상 관대하게 디코딩한다.
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: AuthTokens = AuthTokens()

    override suspend fun readFrom(source: BufferedSource): AuthTokens {
        val content = source.readUtf8()
        if (content.isBlank()) return defaultValue
        return json.decodeFromString(AuthTokens.serializer(), content)
    }

    override suspend fun writeTo(t: AuthTokens, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(AuthTokens.serializer(), t))
    }
}
