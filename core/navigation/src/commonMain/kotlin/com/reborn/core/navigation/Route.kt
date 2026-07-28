package com.reborn.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Intro : Route
    @Serializable data object Aerometer : Route
    // type: "CONSUMER"(소비자 약관) / "POLICY"(이용 정책) / "PRIVACY"(개인정보 처리방침) - 로그인 전(Welcome
    // 화면)/후(설정 화면) 양쪽에서 진입하므로 core:navigation 최상위에 둔다
    @Serializable data class Terms(val type: String = "CONSUMER") : Route

    @Serializable
    sealed interface Admin : Route {
        @Serializable data object Home : Admin
        // deviceId(서버 deviceKey)가 있으면 기기 목록을 건너뛰고 해당 기기의 상세(원격/자동 제어)로
        // 바로 이동(Home IoT 카드 클릭 딥링크, #154/#181)
        @Serializable data class Adjust(val deviceId: String? = null) : Admin
        @Serializable data object IotDeviceList : Admin
        @Serializable data object AddSmartThingsDevice : Admin
        // feedbackId가 있으면 목록을 건너뛰고 해당 피드백 상세로 바로 이동(Home에서 딥링크, #177)
        @Serializable data class Feedback(val feedbackId: Int? = null) : Admin
        @Serializable data object Data : Admin
        @Serializable data object Setting : Admin
        @Serializable data class InviteCode(val placeId: Int) : Admin
        @Serializable data class AddDevice(val placeId: Int) : Admin
        @Serializable data class AddArduino(val placeId: Int) : Admin
        @Serializable data class AddAiSpeaker(val placeId: Int) : Admin
        @Serializable data class DeviceWifiSetup(val deviceId: String) : Admin
    }
}