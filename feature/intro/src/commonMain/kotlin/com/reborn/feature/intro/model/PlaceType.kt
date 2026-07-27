package com.reborn.feature.intro.model

import com.reborn.feature.intro.Res
import com.reborn.feature.intro.*
import org.jetbrains.compose.resources.DrawableResource

// 서버 PlaceType(server/domain/place/PlaceType.kt)과 이름을 맞춘 클라이언트 측 공간 유형 —
// registerPlace 호출 시 name으로 문자열 변환해 서버 계약(PlaceDto.RegisterRequest.type)에 맞춘다.
enum class PlaceType(val image: DrawableResource, val description: String) {
    HOME(Res.drawable.img_home, "나와 가족을 위한 쾌적한 온도/습도 케어"),
    STORE(Res.drawable.img_store, "손님이 머무는 공간의 쾌적함 관리"),
    COMPANY(Res.drawable.img_company, "임직원을 위한 사무 공간 환경 관리")
}
