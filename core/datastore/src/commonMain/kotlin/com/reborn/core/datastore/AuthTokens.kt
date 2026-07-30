package com.reborn.core.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    // 공기계 앱이 페어링(#09) 후 발급받는 기기 자격증명 - WebSocket 인증(Phase 7)에 재사용
    val deviceId: String? = null,
    val appToken: String? = null,
    // 이 앱 인스턴스가 공기계로 페어링됐는지 여부 - 공기계는 로그인을 하지 않으므로(#113) JWT
    // 인터셉터(#121)가 accessToken 유무로 유추하지 않고 이 플래그를 먼저 명시적으로 확인한다.
    val isAerometer: Boolean = false,
    // 최초 접속 튜토리얼(#240)을 끝까지 봤는지 - false인 동안은 Home/Setting/Adjust/Data
    // ViewModel들이 실 API 대신 목데이터를 써서 빈 상태 없이 튜토리얼을 보여준다.
    val tutorialCompleted: Boolean = false,
)
