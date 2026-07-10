package com.reborn.feature.admin.home.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminHomeUiState{
    data object Loading: AdminHomeUiState
    data class Home(
        val placeName: String = "",
        val temperature: Int = 0,
        val humidity: Int = 0,
        val illuminance: Int = 0,
        val peopleCount: Int = 0,
    ): AdminHomeUiState
    data class Alarm(
        val alarm: List<AlarmItem> = emptyList()
    ): AdminHomeUiState

    data class AlarmItem(
        val id: Int,
        val time: String,
        val alarmContent: String
    )
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