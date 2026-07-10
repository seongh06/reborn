package com.reborn.core.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    // 공기계 앱이 페어링(#09) 후 발급받는 기기 자격증명 - WebSocket 인증(Phase 7)에 재사용
    val deviceId: String? = null,
    val appToken: String? = null,
)
