package com.reborn.feature.aerometer.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AerometerUiState {
    data object Loading : AerometerUiState
    data object Home: AerometerUiState
    data object Setting: AerometerUiState
}

sealed interface AerometerIntent{
    data object LoadInitial : AerometerIntent
    data object NavigateToSetting : AerometerIntent
    data object NavigateBack : AerometerIntent
    data object ToggleSaveImage : AerometerIntent
}