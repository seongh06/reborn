package com.reborn.core.datastore

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

internal object AuthTokensSerializer : OkioSerializer<AuthTokens> {

    override val defaultValue: AuthTokens = AuthTokens()

    override suspend fun readFrom(source: BufferedSource): AuthTokens {
        val content = source.readUtf8()
        if (content.isBlank()) return defaultValue
        return Json.decodeFromString(AuthTokens.serializer(), content)
    }

    override suspend fun writeTo(t: AuthTokens, sink: BufferedSink) {
        sink.writeUtf8(Json.encodeToString(AuthTokens.serializer(), t))
    }
}
