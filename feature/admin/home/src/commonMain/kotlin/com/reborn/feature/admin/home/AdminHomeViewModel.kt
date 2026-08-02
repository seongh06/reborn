package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetTutorialSeenStepsUseCase
import com.reborn.core.domain.usecase.MarkTutorialStepSeenUseCase
import com.reborn.core.domain.usecase.ResolveSelectedPlaceUseCase
import com.reborn.core.domain.usecase.SelectPlaceUseCase
import com.reborn.core.model.DomainException
import com.reborn.core.model.Feedback
import com.reborn.core.model.Metric
import com.reborn.core.model.TutorialStep
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.FeedbackListItem
import com.reborn.core.ui.component.RoomOption
import com.reborn.core.ui.component.classifyFeedbackType
import com.reborn.core.ui.component.feedbackStatusToState
import com.reborn.core.ui.component.formatFeedbackRelativeTime
import com.reborn.feature.admin.home.component.IoTDeviceItem
import com.reborn.feature.admin.home.model.AdminHomeIntent
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlinx.coroutines.flow.first
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
    data object NavigateToAddSmartThingsDevice : AdminHomeEvent()
    data class NavigateToDeviceDetail(val deviceId: String) : AdminHomeEvent()
}

private const val RECENT_FEEDBACK_COUNT = 3

// 전용 온습도 센서(ARDUINO)가 에어컨 내장 센서(SMART_THINGS)보다 정확할 가능성이 높아 우선한다 -
// 둘 다 등록된 장소에서 어느 쪽 값이 표시될지가 등록 순서에 따라 뒤바뀌던 문제(#218) 수정.
private val METRIC_DEVICE_TYPE_PRIORITY = listOf("ARDUINO", "SMART_THINGS")

class AdminHomeViewModel(
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val getCurrentMetricUseCase: GetCurrentMetricUseCase,
    private val getFeedbackListUseCase: GetFeedbackListUseCase,
    private val controlDeviceUseCase: ControlDeviceUseCase,
    private val getTutorialSeenStepsUseCase: GetTutorialSeenStepsUseCase,
    private val markTutorialStepSeenUseCase: MarkTutorialStepSeenUseCase,
    private val resolveSelectedPlaceUseCase: ResolveSelectedPlaceUseCase,
    private val selectPlaceUseCase: SelectPlaceUseCase,
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
            is AdminHomeIntent.NavigateToAddSmartThingsDevice -> navigateToAddSmartThingsDevice()
            is AdminHomeIntent.NavigateToDeviceDetail -> navigateToDeviceDetail(intent.deviceId)
            is AdminHomeIntent.TogglePower -> togglePower(intent.deviceId)
            is AdminHomeIntent.NavigateBack -> navController.navigateBack()
            is AdminHomeIntent.NavigateToFeedback -> navigateToFeedbackDetail(intent.feedbackId)
            is AdminHomeIntent.NavigateToFeedbackList -> navigateToFeedbackList()
            is AdminHomeIntent.DeleteAlarm -> deleteAlarm(intent.alarmId)
            is AdminHomeIntent.ClickAlarmFilter -> clickAlarmFilter(intent.filter)
            is AdminHomeIntent.DismissTutorial -> dismissTutorial(intent.stepId)
            is AdminHomeIntent.SelectPlace -> selectPlace(intent.placeId)
            is AdminHomeIntent.Refresh -> refresh()
        }
    }

    // 당겨서 새로고침(pull-to-refresh) - 이미 대시보드를 보고 있는 상태에서만 의미가 있어 Home이
    // 아니면 무시한다. checkInitialState()처럼 전체를 Loading으로 갈아엎지 않고 지금 화면은
    // 그대로 둔 채 isRefreshing만 켜서 상단 인디케이터만 보여준다.
    private fun refresh() {
        if (navController.uiState.value !is AdminHomeUiState.Home) return
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Home)?.copy(isRefreshing = true) ?: state
        }
        viewModelScope.launch {
            navController.clearAndReset(loadHomeState())
        }
    }

    private fun selectPlace(placeId: Long) {
        selectPlaceUseCase(placeId)
        checkInitialState()
    }

    private fun clickAlarmFilter(filter: AdminHomeUiState.AlarmFilter) {
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Alarm)?.copy(filter = filter) ?: state
        }
    }

    private fun checkInitialState() {
        navController.clearAndReset(AdminHomeUiState.Loading)
        viewModelScope.launch {
            navController.clearAndReset(loadHomeState())
        }
    }

    // checkInitialState()(최초 진입 - Loading 경유)와 refresh()(당겨서 새로고침 - 지금 화면 유지)가
    // 공유하는 실제 데이터 조회+상태 조립 로직.
    private suspend fun loadHomeState(): AdminHomeUiState.Home {
            val placeResolution = resolveSelectedPlaceUseCase()
            placeResolution.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val places = placeResolution.getOrNull()?.places.orEmpty()
            val placeId = placeResolution.getOrNull()?.selected?.placeId
            if (placeId == null) {
                return AdminHomeUiState.Home(hasDevices = false)
            }

            val seenSteps = getTutorialSeenStepsUseCase().first()

            val deviceListResult = getDeviceListUseCase(placeId)
            deviceListResult.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val serverDevices = deviceListResult.getOrNull().orEmpty()
            devices = serverDevices.map { device ->
                IoTDeviceItem(
                    id = device.deviceId,
                    place = deviceTypeLabel(device.deviceType),
                    name = device.deviceName ?: device.deviceId,
                    isOnline = device.isOnline,
                    deviceType = resolveDeviceType(device.deviceType, device.category),
                    isPowerOn = false
                )
            }

            // 온습도(아두이노/SmartThings)와 조도·재실 인원(공기계)은 서로 다른 기기가 측정하므로
            // 각자 조회해서 하나의 카드로 합친다 - 이전엔 METRIC_DEVICE_TYPE_PRIORITY에 AEROMETER가
            // 아예 빠져있어서 공기계가 정상적으로 조도/재실 인원을 보내도 홈 화면에 영원히 안 보였음.
            val tempHumidityDevice = METRIC_DEVICE_TYPE_PRIORITY
                .firstNotNullOfOrNull { type -> serverDevices.firstOrNull { it.deviceType == type } }
            val aerometerDevice = serverDevices.firstOrNull { it.deviceType == "AEROMETER" }

            // 신규 등록/오프라인 기기는 아직 metric_logs가 없어 서버가 404(UserNotFoundException으로
            // 매핑됨)를 내려주는데, 이게 홈 탭 재진입마다(#305) 매번 스낵바로 떠서 소음이 됐다 -
            // 이 경우만 조용히 무시하고, 그 외 진짜 네트워크/서버 오류는 그대로 노출한다.
            val tempHumidityMetric = tempHumidityDevice?.let { device ->
                getCurrentMetricUseCase(device.deviceId)
                    .onFailure { if (it !is DomainException.UserNotFoundException) navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
                    .getOrNull()
            }
            val aerometerMetric = aerometerDevice?.let { device ->
                getCurrentMetricUseCase(device.deviceId)
                    .onFailure { if (it !is DomainException.UserNotFoundException) navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
                    .getOrNull()
            }

            val metric = if (tempHumidityMetric == null && aerometerMetric == null) {
                null
            } else {
                Metric(
                    temperature = tempHumidityMetric?.temperature,
                    humidity = tempHumidityMetric?.humidity,
                    illuminance = aerometerMetric?.illuminance,
                    peopleCount = aerometerMetric?.peopleCount,
                )
            }

            val feedbackListResult = getFeedbackListUseCase(placeId)
            feedbackListResult.onFailure { navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it)) }
            val feedbacks = feedbackListResult.getOrNull().orEmpty()
            val sortedFeedbacks = feedbacks.sortedByDescending { it.createdAt }
            val recentFeedbacks = sortedFeedbacks
                .take(RECENT_FEEDBACK_COUNT)
                .map { it.toFeedbackListItem() }
            alarmItems = sortedFeedbacks.map { it.toAlarmItem() }

            return AdminHomeUiState.Home(
                hasDevices = serverDevices.isNotEmpty(),
                // 등록만 되고 WiFi 연결에 한 번도 성공한 적 없는 기기는 홈 카드에서 숨긴다(#276) -
                // 등록 자체는 됐으므로 hasDevices/튜토리얼 판단은 그대로 serverDevices 기준을 쓰고,
                // 전체 기기 목록(설정 화면)에서는 계속 보여서 삭제/재시도가 가능하게 한다.
                devices = devices.filter { it.isOnline },
                metric = metric,
                feedbackTotalCount = feedbacks.size,
                // "미확인 피드백" 배지(#318) - status==PENDING 기준이면 액션 없는(조언만
                // 있는) 피드백은 읽어도 상태가 안 바뀌어 배지에 영원히 남는다. isRead 기준으로
                // 바꿔서 읽으면 바로 빠지도록 수정.
                feedbackWaitingCount = feedbacks.count { !it.isRead },
                recentFeedbacks = recentFeedbacks,
                showTutorialHint = TutorialStep.HOME_SMART_THINGS !in seenSteps && serverDevices.isEmpty(),
                showFirstFeedbackHint = TutorialStep.HOME_FIRST_FEEDBACK !in seenSteps && feedbacks.isNotEmpty(),
                rooms = places.map { RoomOption(id = it.placeId, name = it.name) },
                selectedRoomId = placeId,
            )
    }

    // 서버 device 도메인에 방(room) 개념이 없어(#166) 대신 기기 종류로 그룹/부제목을 표시
    private fun deviceTypeLabel(serverDeviceType: String): String = when (serverDeviceType) {
        "ARDUINO" -> "아두이노"
        "SMART_THINGS" -> "SmartThings"
        "AI_SPEAKER" -> "AI 스피커"
        else -> serverDeviceType
    }

    // ARDUINO/AI_SPEAKER/AEROMETER는 category가 항상 null이라 deviceType으로 직접 분기하고(#298),
    // 그 외(SmartThings)는 등록 시 관리자가 고른 category로 아이콘을 정한다.
    private fun resolveDeviceType(serverDeviceType: String, category: String?): DeviceType = when (serverDeviceType) {
        "ARDUINO" -> DeviceType.ARDUINO
        "AI_SPEAKER" -> DeviceType.AI_SPEAKER
        "AEROMETER" -> DeviceType.AEROMETER
        else -> category?.let { runCatching { DeviceType.valueOf(it) }.getOrNull() } ?: DeviceType.OTHER
    }

    private fun togglePower(deviceId: String) {
        val target = devices.find { it.id == deviceId } ?: return
        val nextPowerOn = !target.isPowerOn

        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = nextPowerOn) else device
        }
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Home)?.copy(devices = devices.filter { it.isOnline }) ?: state
        }

        viewModelScope.launch {
            controlDeviceUseCase(deviceId = deviceId, isPowerOn = nextPowerOn)
                .onFailure {
                    devices = devices.map { device ->
                        if (device.id == deviceId) device.copy(isPowerOn = target.isPowerOn) else device
                    }
                    navController.updateCurrentState { state ->
                        (state as? AdminHomeUiState.Home)?.copy(devices = devices.filter { it.isOnline }) ?: state
                    }
                    navController.emitEvent(AdminHomeEvent.ShowErrorSnackbar(it))
                }
        }
    }

    private fun Feedback.toFeedbackListItem(): FeedbackListItem =
        FeedbackListItem(
            id = feedbackId.toInt(),
            type = classifyFeedbackType(content),
            state = feedbackStatusToState(status, isRead),
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
            time = formatFeedbackRelativeTime(createdAt),
            feedbackType = classifyFeedbackType(content),
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

    private fun navigateToAddSmartThingsDevice() {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToAddSmartThingsDevice)
        }
    }

    private fun navigateToDeviceDetail(deviceId: String) {
        viewModelScope.launch {
            navController.emitEvent(AdminHomeEvent.NavigateToDeviceDetail(deviceId))
        }
    }

    private fun dismissTutorial(stepId: String) {
        navController.updateCurrentState { state ->
            (state as? AdminHomeUiState.Home)?.let {
                when (stepId) {
                    TutorialStep.HOME_SMART_THINGS -> it.copy(showTutorialHint = false)
                    TutorialStep.HOME_FIRST_FEEDBACK -> it.copy(showFirstFeedbackHint = false)
                    else -> it
                }
            } ?: state
        }
        viewModelScope.launch {
            markTutorialStepSeenUseCase(stepId)
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
