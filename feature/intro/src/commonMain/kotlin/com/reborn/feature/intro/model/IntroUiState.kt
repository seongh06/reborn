package com.reborn.feature.intro.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface IntroUiState{
    data object Loading : IntroUiState
    data object Welcome : IntroUiState
    data object Signup: IntroUiState
    data object AerometerPairing: IntroUiState
    data object AerometerDeviceName: IntroUiState
    data object InviteCode: IntroUiState
}

sealed interface IntroIntent{
    data class LoadInitial(val skipToSignup: Boolean = false) : IntroIntent
    data object NavigateToSignup : IntroIntent
    data object NavigateToAerometerPairing : IntroIntent
    data object NavigateToInviteCode : IntroIntent
    data object NavigateToAerometerDeviceName : IntroIntent
    data object NavigateBack : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent
}
