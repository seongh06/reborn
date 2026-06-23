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
    data object AdminCode: IntroUiState
    data object AerometerPairing: IntroUiState
    data object InviteCode: IntroUiState
}

sealed interface IntroIntent{
    data object LoadInitial : IntroIntent
    data object NavigateToTerm : IntroIntent
    data object NavigateToPermission : IntroIntent
    data object NavigateToModeSelect : IntroIntent
    data object NavigateToAdminLogin : IntroIntent
    data object NavigateToAdminModeSelect : IntroIntent
    data object NavigateToAdminPlaceName : IntroIntent
    data object NavigateToAdminPlaceSelect : IntroIntent
    data object NavigateToAerometerPairing : IntroIntent
    data object NavigateToInviteCode : IntroIntent
    data object NavigateToAdminCode : IntroIntent
    data object NavigateBack : IntroIntent
    data object PermissionsGranted : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent

}