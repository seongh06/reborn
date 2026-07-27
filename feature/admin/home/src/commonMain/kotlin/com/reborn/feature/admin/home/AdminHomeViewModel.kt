package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.model.Feedback
import com.reborn.core.ui.component.FeedbackListItem
import com.reborn.core.ui.component.State
import com.reborn.core.ui.component.classifyFeedbackType
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

sealed class AdminHomeEvent {
    data object Exit : AdminHomeEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminHomeEvent()
    data class NavigateToFeedbackDetail(val feedbackId: Int) : AdminHomeEvent()
    data object NavigateToFeedbackList : AdminHomeEvent()
    data object NavigateToSetting : AdminHomeEvent()
    data object NavigateToDeviceList : AdminHomeEvent()
}

private const val RECENT_FEEDBACK_COUNT = 3
private val METRIC_CAPABLE_DEVICE_TYPES = setOf("ARDUINO", "SMART_THINGS")

class AdminHomeViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getCurrentMetricUseCase: GetCurrentMetricUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
) : ViewModel() {
    private val navController = NavigationManager<AdminHomeUiState, AdminHomeEvent>(
        initialState = AdminHomeUiState.Loading,
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
            val placeId = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
            if (placeId == null) {
                navController.clearAndReset(AdminHomeUiState.Home(hasDevices = false))
                return@launch
            }

            val devices = getDeviceListUseCase(placeId).getOrNull().orEmpty()
            val metric = devices.firstOrNull { it.deviceType in METRIC_CAPABLE_DEVICE_TYPES }
                ?.let { device -> getCurrentMetricUseCase(device.deviceId).getOrNull() }

            val feedbacks = getFeedbackListUseCase(placeId).getOrNull().orEmpty()
            val recentFeedbacks = feedbacks
                .sortedByDescending { it.createdAt }
                .take(RECENT_FEEDBACK_COUNT)
                .map { it.toFeedbackListItem() }

            navController.clearAndReset(
                AdminHomeUiState.Home(
                    hasDevices = devices.isNotEmpty(),
                    metric = metric,
                    feedbackTotalCount = feedbacks.size,
                    feedbackWaitingCount = feedbacks.count { it.status == "PENDING" },
                    recentFeedbacks = recentFeedbacks
                )
            )
        }
    }

    private fun Feedback.toFeedbackListItem(): FeedbackListItem =
        FeedbackListItem(
            id = feedbackId.toInt(),
            type = classifyFeedbackType(content),
            state = when (status) {
                "APPROVED" -> State.APPROVE
                "REJECTED" -> State.REJECT
                else -> State.WAITING
            },
            time = formatRelativeTime(createdAt),
            title = content
        )

    private fun formatRelativeTime(iso: String): String {
        val createdInstant = LocalDateTime.parse(iso).toInstant(TimeZone.currentSystemDefault())
        val minutes = (Clock.System.now() - createdInstant).inWholeMinutes
        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분전"
            minutes < 60 * 24 -> "${minutes / 60}시간전"
            minutes < 60 * 24 * 2 -> "어제"
            else -> "${minutes / (60 * 24)}일전"
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
