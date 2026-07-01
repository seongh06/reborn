package com.reborn.feature.admin.home.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminHomeUiState{
    data object Loading: AdminHomeUiState
    data object Home: AdminHomeUiState
    data object Alarm: AdminHomeUiState
    data object Setting: AdminHomeUiState
}

sealed interface AdminHomeIntent{
    data object LoadInitial : AdminHomeIntent
    data object NavigateToAlarm : AdminHomeIntent
    data object NavigateToSetting : AdminHomeIntent
    data object NavigateBack : AdminHomeIntent
}