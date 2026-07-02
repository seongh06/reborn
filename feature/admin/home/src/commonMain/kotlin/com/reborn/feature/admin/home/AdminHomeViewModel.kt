package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AdminHomeEvent {
    data object Exit : AdminHomeEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminHomeEvent()
    data class NavigateToFeedbackDetail(val feedbackId: Int) : AdminHomeEvent()
}

class AdminHomeViewModel : ViewModel() {
    private val navController = NavigationManager<AdminHomeUiState, AdminHomeEvent>(
        initialState = AdminHomeUiState.Home,
        exitEvent = AdminHomeEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    private var alarmItems: List<AdminHomeUiState.AlarmItem> = emptyList()

    fun onIntent(intent: AdminHomeIntent) {
        when (intent) {
            is AdminHomeIntent.LoadInitial -> checkInitialState()
            is AdminHomeIntent.NavigateToAlarm -> navController.navigateTo(AdminHomeUiState.Alarm(alarm = alarmItems))
            is AdminHomeIntent.NavigateToSetting -> navController.navigateTo(AdminHomeUiState.Setting)
            is AdminHomeIntent.NavigateBack -> navController.navigateBack()
            is AdminHomeIntent.NavigateToFeedback -> navigateToFeedbackDetail(intent.feedbackId)
            is AdminHomeIntent.DeleteAlarm -> deleteAlarm(intent.alarmId)
            is AdminHomeIntent.DeleteAllAlarms -> deleteAllAlarms()
        }
    }

    private fun checkInitialState() {
        navController.clearAndReset(AdminHomeUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            navController.clearAndReset(AdminHomeUiState.Home)
        }
    }

    private fun navigateToFeedbackDetail(feedbackId: Int) {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToFeedbackDetail(feedbackId))
        }
    }

    private fun deleteAlarm(alarmId: Int) {
        alarmItems = alarmItems.filter { it.id != alarmId }
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Alarm)
                ?.copy(alarm = alarmItems)
                ?: state
        }
    }

    private fun deleteAllAlarms() {
        alarmItems = emptyList()
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Alarm)
                ?.copy(alarm = alarmItems)
                ?: state
        }
    }
}
