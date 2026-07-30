package com.reborn.feature.intro.model

// 서버 PlaceType(server/domain/place/PlaceType.kt)과 이름을 맞춘 클라이언트 측 공간 유형 —
// registerPlace 호출 시 enum name(HOME/STORE/COMPANY)으로 문자열 변환해 서버 계약
// (PlaceDto.RegisterRequest.type)에 맞춘다 - label은 화면 표시 전용이라 enum name과 분리.
enum class PlaceType(val label: String, val description: String) {
    HOME("가정용", "나와 가족을 위한 쾌적한 온도/습도 케어"),
    STORE("매장용", "손님이 머무는 공간의 쾌적함 관리"),
    COMPANY("기업용", "임직원을 위한 사무 공간 환경 관리")
}
