package com.reborn.feature.intro.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
sealed interface IntroUiState{
    data object Loading : IntroUiState
}

sealed interface IntroIntent{
    data object LoadInitial : IntroIntent
    data object NavigateToAdmin : IntroIntent
    data object NavigateToAerometer : IntroIntent

}