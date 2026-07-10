package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

sealed class AdminHomeEvent {
    data object Exit : AdminHomeEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminHomeEvent()
    data class NavigateToFeedbackDetail(val feedbackId: Int) : AdminHomeEvent()
    data object NavigateToSetting : AdminHomeEvent()
}

class AdminHomeViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getCurrentMetricUseCase: GetCurrentMetricUseCase,
) : ViewModel() {
    private val navController = NavigationManager<AdminHomeUiState, AdminHomeEvent>(
        initialState = AdminHomeUiState.Home(),
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
            is AdminHomeIntent.NavigateToSetting -> navigateToSetting()
            is AdminHomeIntent.NavigateBack -> navController.navigateBack()
            is AdminHomeIntent.NavigateToFeedback -> navigateToFeedbackDetail(intent.feedbackId)
            is AdminHomeIntent.DeleteAlarm -> deleteAlarm(intent.alarmId)
            is AdminHomeIntent.DeleteAllAlarms -> deleteAllAlarms()
        }
    }

    private fun checkInitialState() {
        navController.clearAndReset(AdminHomeUiState.Loading)
        viewModelScope.launch {
            val places = getPlaceListUseCase().getOrElse {
                navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it))
                navController.clearAndReset(AdminHomeUiState.Home())
                return@launch
            }
            val place = places.firstOrNull()
            if (place == null) {
                navController.clearAndReset(AdminHomeUiState.Home())
                return@launch
            }

            // 대시보드는 관리자가 접근 가능한 첫 번째 장소의 ARDUINO 센서 기기 값을 표시한다.
            // 관리자 앱이 여러 장소를 오가는 UI가 아직 없어 임시로 첫 장소를 사용(#124)
            val device = getDeviceListUseCase(place.placeId).getOrNull()
                ?.firstOrNull { it.deviceType == "ARDUINO" }

            if (device == null) {
                navController.clearAndReset(AdminHomeUiState.Home(placeName = place.name))
                return@launch
            }

            val metric = getCurrentMetricUseCase(device.deviceId).getOrNull()
            navController.clearAndReset(
                AdminHomeUiState.Home(
                    placeName = place.name,
                    temperature = metric?.temperature?.roundToInt() ?: 0,
                    humidity = metric?.humidity?.roundToInt() ?: 0,
                    illuminance = metric?.illuminance ?: 0,
                    peopleCount = metric?.peopleCount ?: 0,
                )
            )
        }
    }

    private fun navigateToFeedbackDetail(feedbackId: Int) {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToFeedbackDetail(feedbackId))
        }
    }

    private fun navigateToSetting() {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToSetting)
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
