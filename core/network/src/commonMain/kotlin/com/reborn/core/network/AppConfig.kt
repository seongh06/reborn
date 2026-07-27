package com.reborn.core.network

// BuildConfig.BASE_URL은 이 모듈 내부 전용(internal)이라, 다른 모듈에서 API 서버 도메인이
// 필요할 때(ex. QR 피드백 웹페이지 링크 생성, #163) 이 공개 래퍼를 통해 참조한다.
object AppConfig {
    val webBaseUrl: String = BuildConfig.BASE_URL
}
