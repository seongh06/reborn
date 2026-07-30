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
    // 최초 접속 튜토리얼(#240) - 화면마다 독립적인 코치마크라 각 단계를 봤는지 개별로 기록한다
    // (TutorialStep의 id 문자열). 순서 상관없이 아무 화면이나 먼저 방문해도 그 화면의
    // 코치마크만 뜨고, 이미 본 단계는 다시 안 뜬다.
    val tutorialSeenSteps: Set<String> = emptySet(),
)
