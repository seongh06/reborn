package com.reborn.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Intro : Route
    @Serializable data object Aerometer : Route

    @Serializable
    sealed interface Admin : Route {
        @Serializable data object Home : Admin
        @Serializable data object Adjust : Admin
        @Serializable data object IotDeviceList : Admin
        // feedbackId가 있으면 목록을 건너뛰고 해당 피드백 상세로 바로 이동(Home에서 딥링크, #177)
        @Serializable data class Feedback(val feedbackId: Int? = null) : Admin
        @Serializable data object Data : Admin
        @Serializable data object Setting : Admin
        @Serializable data class InviteCode(val placeId: Int) : Admin
        @Serializable data class AddDevice(val placeId: Int) : Admin
        @Serializable data class AddArduino(val placeId: Int) : Admin
        @Serializable data class AddAiSpeaker(val placeId: Int) : Admin
    }
}