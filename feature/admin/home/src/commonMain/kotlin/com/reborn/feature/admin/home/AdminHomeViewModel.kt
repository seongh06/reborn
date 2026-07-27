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
    data object NavigateToFeedbackList : AdminHomeEvent()
    data object NavigateToSetting : AdminHomeEvent()
    data object NavigateToDeviceList : AdminHomeEvent()
}

class AdminHomeViewModel : ViewModel() {
    private val navController = NavigationManager<AdminHomeUiState, AdminHomeEvent>(
        initialState = AdminHomeUiState.Home,
        exitEvent = AdminHomeEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    // TODO: 서버 알림 API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정 - Figma 595:5087 그대로
    private var alarmItems: List<AdminHomeUiState.AlarmItem> = listOf(
        AdminHomeUiState.AlarmItem(
            id = 1,
            group = AdminHomeUiState.AlarmGroup.THIS_WEEK,
            category = AdminHomeUiState.AlarmFilter.SETTLEMENT,
            title = "2026.08 receipt",
            content = "이번 달의 월말결산을 확인해보세요",
            time = "2일 전"
        ),
        AdminHomeUiState.AlarmItem(
            id = 2,
            group = AdminHomeUiState.AlarmGroup.THIS_WEEK,
            category = AdminHomeUiState.AlarmFilter.FEEDBACK,
            content = "최근에 피드백을 받았습니다!"
        ),
        AdminHomeUiState.AlarmItem(
            id = 3,
            group = AdminHomeUiState.AlarmGroup.PREVIOUS,
            category = AdminHomeUiState.AlarmFilter.FEEDBACK,
            content = "최근에 피드백을 받았습니다!"
        ),
        AdminHomeUiState.AlarmItem(
            id = 4,
            group = AdminHomeUiState.AlarmGroup.PREVIOUS,
            category = AdminHomeUiState.AlarmFilter.SETTLEMENT,
            title = "2026.07 receipt",
            content = "이번 달의 월말결산을 확인해보세요",
            time = "한달 전"
        ),
    )

    fun onIntent(intent: AdminHomeIntent) {
        when (intent) {
            is AdminHomeIntent.LoadInitial -> checkInitialState()
            is AdminHomeIntent.NavigateToAlarm -> navController.navigateTo(AdminHomeUiState.Alarm(alarm = alarmItems))
            is AdminHomeIntent.NavigateToSetting -> navigateToSetting()
            is AdminHomeIntent.NavigateToDeviceList -> navigateToDeviceList()
            is AdminHomeIntent.NavigateBack -> navController.navigateBack()
            is AdminHomeIntent.NavigateToFeedback -> navigateToFeedbackDetail(intent.feedbackId)
            is AdminHomeIntent.NavigateToFeedbackList -> navigateToFeedbackList()
            is AdminHomeIntent.DeleteAlarm -> deleteAlarm(intent.alarmId)
            is AdminHomeIntent.ClickAlarmFilter -> clickAlarmFilter(intent.filter)
        }
    }

    private fun clickAlarmFilter(filter: AdminHomeUiState.AlarmFilter) {
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Alarm)?.copy(filter = filter) ?: state
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

    private fun navigateToFeedbackList() {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToFeedbackList)
        }
    }

    private fun navigateToSetting() {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToSetting)
        }
    }

    private fun navigateToDeviceList() {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToDeviceList)
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

}
