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

    // 피드백 도착 이벤트를 알림으로 재사용(#191) - "결산"(SETTLEMENT)류 알림은 그 근거가 되는 월말결산
    // 기능 자체가 서버에 없어서 만들어낼 데이터가 없다. 알림 목록에는 FEEDBACK 카테고리만 실제로 채워지고,
    // SETTLEMENT 필터 칩을 선택하면(정직하게) 빈 목록이 뜬다.
    private var alarmItems: List<AdminHomeUiState.AlarmItem> = emptyList()

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
            val sortedFeedbacks = feedbacks.sortedByDescending { it.createdAt }
            val recentFeedbacks = sortedFeedbacks
                .take(RECENT_FEEDBACK_COUNT)
                .map { it.toFeedbackListItem() }
            alarmItems = sortedFeedbacks.map { it.toAlarmItem() }

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

    private fun Feedback.toAlarmItem(): AdminHomeUiState.AlarmItem {
        val createdInstant = LocalDateTime.parse(createdAt).toInstant(TimeZone.currentSystemDefault())
        val daysAgo = (Clock.System.now() - createdInstant).inWholeDays
        val group = if (daysAgo < 7) AdminHomeUiState.AlarmGroup.THIS_WEEK else AdminHomeUiState.AlarmGroup.PREVIOUS
        return AdminHomeUiState.AlarmItem(
            id = feedbackId.toInt(),
            group = group,
            category = AdminHomeUiState.AlarmFilter.FEEDBACK,
            content = "새로운 피드백이 도착했습니다: $content",
            time = formatFeedbackRelativeTime(createdAt)
        )
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
