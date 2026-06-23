package com.reborn.feature.intro.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface IntroUiState{
    data object Loading : IntroUiState
    data object Start : IntroUiState
    data object Term: IntroUiState
    data object Permission: IntroUiState
}

sealed interface IntroIntent{
    data object LoadInitial : IntroIntent
    data object NavigateToTerm : IntroIntent
    data object NavigateToPermission : IntroIntent
    data object PermissionsGranted : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent

}