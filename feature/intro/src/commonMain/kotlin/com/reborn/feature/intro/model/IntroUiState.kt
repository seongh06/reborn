package com.reborn.feature.intro.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface IntroUiState{
    data object Loading : IntroUiState
    data object Start : IntroUiState
    data object Term: IntroUiState
    data object Permission: IntroUiState
    data object ModeSelect: IntroUiState
    data object AdminLogin: IntroUiState
    data object AdminModeSelect: IntroUiState
    data object AdminPlaceName: IntroUiState
    data object AdminPlaceSelect: IntroUiState
    // 장소 생성 직후 공기계 기기를 이 장소에 연결하기 위한 페어링 코드 발급 화면 (관리자 초대 코드 아님 - #110)
    data object DevicePairing: IntroUiState
    data object AerometerPairing: IntroUiState
    data object AerometerDeviceName: IntroUiState
    data object InviteCode: IntroUiState
}

sealed interface IntroIntent{
    data class LoadInitial(val skipToAdminModeSelect: Boolean = false) : IntroIntent
    data object NavigateToTerm : IntroIntent
    data object NavigateToPermission : IntroIntent
    data object NavigateToModeSelect : IntroIntent
    data object NavigateToAdminLogin : IntroIntent
    data object NavigateToAdminModeSelect : IntroIntent
    data object NavigateToAdminPlaceName : IntroIntent
    data object NavigateToAdminPlaceSelect : IntroIntent
    data object NavigateToAerometerPairing : IntroIntent
    data object NavigateToInviteCode : IntroIntent
    data object NavigateToDevicePairing : IntroIntent
    data object NavigateToAerometerDeviceName : IntroIntent
    data object NavigateBack : IntroIntent
    data object PermissionsGranted : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent

}