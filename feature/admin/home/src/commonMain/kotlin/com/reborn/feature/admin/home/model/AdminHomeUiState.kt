package com.reborn.feature.admin.home.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminHomeUiState{
    data object Loading: AdminHomeUiState
    data object Home: AdminHomeUiState
    data class Alarm(
        val alarm: List<AlarmItem> = emptyList()
    ): AdminHomeUiState

    data class AlarmItem(
        val id: Int,
        val time: String,
        val alarmContent: String
    )

    data object Setting: AdminHomeUiState
}

sealed interface AdminHomeIntent{
    data object LoadInitial : AdminHomeIntent
    data object NavigateToAlarm : AdminHomeIntent
    data object NavigateToSetting : AdminHomeIntent
    data object NavigateBack : AdminHomeIntent
    data class NavigateToFeedback(val feedbackId: Int): AdminHomeIntent
    data class DeleteAlarm(val alarmId: Int) : AdminHomeIntent
    data object DeleteAllAlarms : AdminHomeIntent
}