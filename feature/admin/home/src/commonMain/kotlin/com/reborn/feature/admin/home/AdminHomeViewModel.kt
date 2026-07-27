package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.model.Feedback
import com.reborn.core.ui.component.FeedbackListItem
import com.reborn.core.ui.component.classifyFeedbackType
import com.reborn.core.ui.component.feedbackStatusToState
import com.reborn.core.ui.component.formatFeedbackRelativeTime
import com.reborn.feature.admin.home.component.IoTDeviceItem
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlinx.coroutines.launch

sealed class AdminHomeEvent {
    data object Exit : AdminHomeEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminHomeEvent()
    data class NavigateToFeedbackDetail(val feedbackId: Int) : AdminHomeEvent()
    data object NavigateToFeedbackList : AdminHomeEvent()
    data object NavigateToSetting : AdminHomeEvent()
    data object NavigateToDeviceList : AdminHomeEvent()
    data class NavigateToDeviceDetail(val deviceId: String) : AdminHomeEvent()
}

private const val RECENT_FEEDBACK_COUNT = 3
private val METRIC_CAPABLE_DEVICE_TYPES = setOf("ARDUINO", "SMART_THINGS")

class AdminHomeViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getCurrentMetricUseCase: GetCurrentMetricUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
    private val controlDeviceUseCase: ControlDeviceUseCase,
) : ViewModel() {
    private val navController = NavigationManager<AdminHomeUiState, AdminHomeEvent>(
        initialState = AdminHomeUiState.Loading,
        exitEvent = AdminHomeEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    private var devices: List<IoTDeviceItem> = emptyList()

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
            is AdminHomeIntent.NavigateToDeviceDetail -> navigateToDeviceDetail(intent.deviceId)
            is AdminHomeIntent.TogglePower -> togglePower(intent.deviceId)
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
            val placeListResult = getPlaceListUseCase()
            placeListResult.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val placeId = placeListResult.getOrNull()?.firstOrNull()?.placeId
            if (placeId == null) {
                navController.clearAndReset(AdminHomeUiState.Home(hasDevices = false))
                return@launch
            }

            val deviceListResult = getDeviceListUseCase(placeId)
            deviceListResult.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val serverDevices = deviceListResult.getOrNull().orEmpty()
            devices = serverDevices.map { device ->
                IoTDeviceItem(
                    id = device.deviceId,
                    place = deviceTypeLabel(device.deviceType),
                    name = device.deviceName ?: device.deviceId,
                    isOnline = device.isOnline,
                    isPowerOn = false
                )
            }

            val metric = serverDevices.firstOrNull { it.deviceType in METRIC_CAPABLE_DEVICE_TYPES }
                ?.let { device ->
                    getCurrentMetricUseCase(device.deviceId)
                        .onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
                        .getOrNull()
                }

            val feedbackListResult = getFeedbackListUseCase(placeId)
            feedbackListResult.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val feedbacks = feedbackListResult.getOrNull().orEmpty()
            val recentFeedbacks = feedbacks
                .sortedByDescending { it.createdAt }
                .take(RECENT_FEEDBACK_COUNT)
                .map { it.toFeedbackListItem() }

            navController.clearAndReset(
                AdminHomeUiState.Home(
                    hasDevices = serverDevices.isNotEmpty(),
                    devices = devices,
                    metric = metric,
                    feedbackTotalCount = feedbacks.size,
                    feedbackWaitingCount = feedbacks.count { it.status == "PENDING" },
                    recentFeedbacks = recentFeedbacks
                )
            )
        }
    }

    // 서버 device 도메인에 방(room) 개념이 없어(#166) 대신 기기 종류로 그룹/부제목을 표시
    private fun deviceTypeLabel(serverDeviceType: String): String = when (serverDeviceType) {
        "ARDUINO" -> "아두이노"
        "SMART_THINGS" -> "SmartThings"
        "AI_SPEAKER" -> "AI 스피커"
        else -> serverDeviceType
    }

    private fun togglePower(deviceId: String) {
        val target = devices.find { it.id == deviceId } ?: return
        val nextPowerOn = !target.isPowerOn

        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = nextPowerOn) else device
        }
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Home)?.copy(devices = devices) ?: state
        }

        viewModelScope.launch {
            controlDeviceUseCase(deviceId = deviceId, isPowerOn = nextPowerOn)
                .onFailure {
                    devices = devices.map { device ->
                        if (device.id == deviceId) device.copy(isPowerOn = target.isPowerOn) else device
                    }
                    navController.updateCurrentState { state ->
                        (state as? AdminHomeUiState.Home)?.copy(devices = devices) ?: state
                    }
                    navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it))
                }
        }
    }

    private fun Feedback.toFeedbackListItem(): FeedbackListItem =
        FeedbackListItem(
            id = feedbackId.toInt(),
            type = classifyFeedbackType(content),
            state = feedbackStatusToState(status),
            time = formatFeedbackRelativeTime(createdAt),
            title = content
        )

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

    private fun navigateToDeviceDetail(deviceId: String) {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToDeviceDetail(deviceId))
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
