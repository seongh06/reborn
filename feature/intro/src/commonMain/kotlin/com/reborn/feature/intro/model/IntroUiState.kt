package com.reborn.feature.intro.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface IntroUiState{
    data object Loading : IntroUiState
    data object Welcome : IntroUiState
    data object Signup: IntroUiState
    // 장소 생성 직후 공기계 기기를 이 장소에 연결하기 위한 페어링 코드 발급 화면 (관리자 초대 코드 아님 - #110)
    data object DevicePairing: IntroUiState
    data object AerometerPairing: IntroUiState
    data object AerometerDeviceName: IntroUiState
    data object InviteCode: IntroUiState
}

sealed interface IntroIntent{
    data class LoadInitial(val skipToSignup: Boolean = false) : IntroIntent
    data object NavigateToSignup : IntroIntent
    data object NavigateToAerometerPairing : IntroIntent
    data object NavigateToInviteCode : IntroIntent
    data object NavigateToDevicePairing : IntroIntent
    data object NavigateToAerometerDeviceName : IntroIntent
    data object NavigateBack : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent
}
