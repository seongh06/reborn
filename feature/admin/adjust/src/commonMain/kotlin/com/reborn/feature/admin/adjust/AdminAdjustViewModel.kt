package com.reborn.feature.admin.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.DeleteDeviceUseCase
import com.reborn.core.domain.usecase.GetAutoControlRuleUseCase
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetDeviceStatusUseCase
import com.reborn.core.domain.usecase.GetTutorialSeenStepsUseCase
import com.reborn.core.domain.usecase.MarkTutorialStepSeenUseCase
import com.reborn.core.domain.usecase.ResolveSelectedPlaceUseCase
import com.reborn.core.domain.usecase.SaveAutoControlRuleUseCase
import com.reborn.core.model.AutoControlRule
import com.reborn.core.model.TutorialStep
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import com.reborn.feature.admin.adjust.model.DeviceCurrentStatus
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed
import com.reborn.feature.admin.adjust.model.defaultAutoControlState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AdminAdjustEvent {
    data object Exit : AdminAdjustEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAdjustEvent()
    data class ShowSnackbar(val message: String) : AdminAdjustEvent()
}

class AdminAdjustViewModel(
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val controlDeviceUseCase: ControlDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val saveAutoControlRuleUseCase: SaveAutoControlRuleUseCase,
    private val getAutoControlRuleUseCase: GetAutoControlRuleUseCase,
    private val getCurrentMetricUseCase: GetCurrentMetricUseCase,
    private val getDeviceStatusUseCase: GetDeviceStatusUseCase,
    private val getTutorialSeenStepsUseCase: GetTutorialSeenStepsUseCase,
    private val markTutorialStepSeenUseCase: MarkTutorialStepSeenUseCase,
    private val resolveSelectedPlaceUseCase: ResolveSelectedPlaceUseCase,
) : ViewModel() {
    private val navController = NavigationManager<AdminAdjustUiState, AdminAdjustEvent>(
        initialState = AdminAdjustUiState.Loading,
        exitEvent = AdminAdjustEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    private var devices: List<AdminAdjustUiState.DeviceItem> = emptyList()

    // 최초 접속 튜토리얼(#240) - checkInitialState()에서 한 번 읽어두고, 원격/자동제어 탭
    // 하이라이트 여부를 DeviceDetail 진입/전환 시마다 이 값으로 판단한다.
    private var seenTutorialSteps: Set<String> = emptySet()

    // Home/Data에서 지금 선택된 룸(#166)을 그대로 따른다 - ResolveSelectedPlaceUseCase가 선택한
    // 적 없거나 삭제된 룸을 자동으로 첫 번째 룸으로 폴백시켜준다. 이전엔 항상 첫 번째 룸만 봐서,
    // 다른 룸에 등록한 기기를 클릭해도 그 룸 목록엔 없어 "기기를 찾을 수 없습니다" 에러만 뜨고
    // 상세 화면으로 못 넘어가던 버그가 있었다.
    private var resolvedPlaceId: Long? = null

    // 장소 조회 자체가 실패한 경우(네트워크 오류 등)를 "장소 없음"으로 뭉개면 사용자가 진짜 원인을
    // 못 보고 오해한다(CodeRabbit 리뷰) - Result를 그대로 호출부까지 전달해서 실패/빈 목록을 구분한다.
    private suspend fun resolvePlaceId(): Result<Long?> {
        resolvedPlaceId?.let { return Result.success(it) }
        return resolveSelectedPlaceUseCase().map { resolution ->
            resolution.selected?.placeId?.also { resolvedPlaceId = it }
        }
    }

    // 캐시해둔 placeId가 가리키는 장소가 그 사이 삭제되는 등으로 이 값을 쓰는 호출이 실패하면
    // 캐시를 지워서 다음 진입 시 장소 목록을 다시 조회하게 한다 - 캐시가 죽은 채로 남아있으면
    // 장소가 삭제된 뒤에도 이 화면 전체가 계속 실패한다(#232).
    private fun invalidatePlaceId() {
        resolvedPlaceId = null
    }

    // 동일 기기에 토글/원격 제어가 겹쳐 들어오면 이전 요청의 응답이 나중에 도착해 UI가
    // 실제 상태와 어긋날 수 있어(CodeRabbit #178) 기기별로 진행 중인 제어 요청을 직렬화한다.
    private val pendingControlJobs = mutableMapOf<String, Job>()

    private fun launchControl(deviceId: String, block: suspend () -> Unit) {
        pendingControlJobs[deviceId]?.cancel()
        pendingControlJobs[deviceId] = viewModelScope.launch {
            block()
            pendingControlJobs.remove(deviceId)
        }
    }

    fun onIntent(intent: AdminAdjustIntent) {
        when (intent) {
            is AdminAdjustIntent.LoadInitial -> checkInitialState(intent.deviceId)
            is AdminAdjustIntent.NavigateBack -> navigateBack()
            is AdminAdjustIntent.NavigateToAddDevice -> navController.navigateTo(AdminAdjustUiState.AddDevice)
            is AdminAdjustIntent.NavigateToDeviceDetail -> navigateToDeviceDetail(intent)
            is AdminAdjustIntent.TogglePower -> togglePower(intent.deviceId)
            is AdminAdjustIntent.AddDevice -> addDevice(intent.place, intent.name)
            is AdminAdjustIntent.ClickTab -> handleTabClick(intent.tab)
            is AdminAdjustIntent.SendRemoteControl -> sendRemoteControl(intent)
            is AdminAdjustIntent.SendAutoControl -> sendAutoControl(intent)
            is AdminAdjustIntent.DeleteDevice -> deleteDevice(intent.deviceId)
            is AdminAdjustIntent.DismissTutorial -> dismissTutorial(intent.stepId)
        }
    }

    private fun dismissTutorial(stepId: String) {
        navController.updateCurrentState { state ->
            (state as? AdminAdjustUiState.DeviceDetail)?.let {
                when (stepId) {
                    TutorialStep.ADJUST_REMOTE_TAB -> it.copy(showRemoteTabHint = false)
                    TutorialStep.ADJUST_AUTO_TAB -> it.copy(showAutoTabHint = false)
                    else -> it
                }
            } ?: state
        }
        viewModelScope.launch {
            markTutorialStepSeenUseCase(stepId)
        }
    }

    private fun checkInitialState(deviceId: String? = null) {
        navController.clearAndReset(AdminAdjustUiState.Loading)
        viewModelScope.launch {
            seenTutorialSteps = getTutorialSeenStepsUseCase().first()
            val placeResult = resolvePlaceId()
            placeResult.onFailure { navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it)) }
            val placeId = placeResult.getOrNull()
            if (placeId == null) {
                if (placeResult.isSuccess) {
                    navController.emitEvent(
                        AdminAdjustEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다."))
                    )
                }
                exitOrShowEmptyList(deviceId)
                return@launch
            }

            getDeviceListUseCase(placeId)
                .onSuccess { list ->
                    devices = list.map { device ->
                        AdminAdjustUiState.DeviceItem(
                            id = device.deviceId,
                            place = deviceTypeLabel(device.deviceType),
                            name = device.deviceName ?: device.deviceId,
                            isOnline = device.isOnline,
                            deviceType = resolveDeviceType(device.deviceType, device.category),
                            serverDeviceType = device.deviceType,
                        )
                    }
                    // 이 화면(Adjust 목록)은 바텀탭에서 빠져서 실제로는 항상 특정 기기(deviceId)로만
                    // 진입한다 - 목록 상태를 거치지 않고 바로 상세로 간다. 실패하면(기기를 못 찾음)
                    // 아무 역할 없는 빈 목록 화면 대신 바로 나간다("기기 화면이 하는 역할이 없다"는
                    // 사용자 피드백 - 이전엔 실패 시 항상 목록 화면이 남아있었음).
                    if (deviceId != null) {
                        val device = devices.find { it.id == deviceId }
                        if (device == null) {
                            navController.emitEvent(
                                AdminAdjustEvent.ShowErrorSnackbar(IllegalArgumentException("기기를 찾을 수 없습니다."))
                            )
                            navController.emitEvent(AdminAdjustEvent.Exit)
                            return@onSuccess
                        }
                        openDeviceDetail(device, AdminAdjustUiState.ControlMethod.Remote)
                    } else {
                        navController.clearAndReset(AdminAdjustUiState.Adjust(devices))
                    }
                }
                .onFailure {
                    invalidatePlaceId()
                    navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it))
                    exitOrShowEmptyList(deviceId)
                }
        }
    }

    private fun exitOrShowEmptyList(deviceId: String?) {
        if (deviceId != null) {
            navController.emitEvent(AdminAdjustEvent.Exit)
        } else {
            navController.clearAndReset(AdminAdjustUiState.Adjust(emptyList()))
        }
    }

    // IoT 기기 상세에서 뒤로가기는 내부적으로 쌓인 목록 화면을 거치지 않고 항상 바로 HomeScreen으로
    // 나가야 한다(#235) - 목록 화면(Adjust)으로 한 단계만 돌아가면 사용자가 요청하지 않은 중간
    // 화면을 거치게 되므로, DeviceDetail에서는 내부 스택을 건너뛰고 바로 Exit을 emit한다.
    private fun navigateBack() {
        if (navController.uiState.value is AdminAdjustUiState.DeviceDetail) {
            navController.emitEvent(AdminAdjustEvent.Exit)
        } else {
            navController.navigateBack()
        }
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

    private fun navigateToDeviceDetail(intent: AdminAdjustIntent.NavigateToDeviceDetail) {
        val device = devices.find { it.id == intent.deviceId }
            ?: return navController.emitEvent(
                AdminAdjustEvent.ShowErrorSnackbar(IllegalArgumentException("기기를 찾을 수 없습니다."))
            )
        openDeviceDetail(device, intent.controlMethod)
    }

    private fun openDeviceDetail(device: AdminAdjustUiState.DeviceItem, controlMethod: AdminAdjustUiState.ControlMethod) {
        navController.navigateTo(
            AdminAdjustUiState.DeviceDetail(
                selectedTab = controlMethod,
                deviceId = device.id,
                device = device,
                showRemoteTabHint = TutorialStep.ADJUST_REMOTE_TAB !in seenTutorialSteps,
                showAutoTabHint = TutorialStep.ADJUST_AUTO_TAB !in seenTutorialSteps,
            )
        )
        loadData(controlMethod)
        loadMetric(device.id)
        if (device.serverDeviceType == "SMART_THINGS") {
            loadDeviceStatus(device.id)
        }
    }

    // 현재 센서 상태(#221) - 실패해도 조용히 무시하고 하드코딩 기본값(0으로 채워지던 예전 동작)
    // 대신 그냥 칩을 안 보여주는 쪽을 택한다(화면 자체를 막지 않기 위함).
    private fun loadMetric(deviceId: String) {
        viewModelScope.launch {
            getCurrentMetricUseCase(deviceId)
                .onSuccess { metric ->
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.DeviceDetail)
                            ?.takeIf { it.deviceId == deviceId }
                            ?.copy(metric = metric)
                            ?: state
                    }
                }
        }
    }

    // 원격 제어 패널의 현재 전원/운전모드/바람세기/희망온도(#221) - SMART_THINGS 기기에서만 호출.
    private fun loadDeviceStatus(deviceId: String) {
        viewModelScope.launch {
            getDeviceStatusUseCase(deviceId)
                .onSuccess { status ->
                    val current = DeviceCurrentStatus(
                        isPowerOn = status.isPowerOn,
                        operationMode = status.operationMode?.let { raw ->
                            runCatching { OperationMode.valueOf(raw) }.getOrNull()
                        },
                        windSpeed = status.windSpeed?.let { raw ->
                            runCatching { WindSpeed.valueOf(raw) }.getOrNull()
                        },
                        temperature = status.temperature?.toFloat(),
                    )
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.DeviceDetail)
                            ?.takeIf { it.deviceId == deviceId }
                            ?.copy(deviceStatus = current)
                            ?: state
                    }
                }
        }
    }

    private fun togglePower(deviceId: String) {
        val target = devices.find { it.id == deviceId } ?: return
        val nextPowerOn = !target.isPowerOn

        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = nextPowerOn) else device
        }
        navController.updateCurrentState { state ->
            (state as? AdminAdjustUiState.Adjust)?.copy(devices = devices) ?: state
        }

        launchControl(deviceId) {
            controlDeviceUseCase(deviceId = deviceId, isPowerOn = nextPowerOn)
                .onFailure {
                    // 실패 시 낙관적으로 바꿔둔 UI 상태를 되돌린다
                    devices = devices.map { device ->
                        if (device.id == deviceId) device.copy(isPowerOn = target.isPowerOn) else device
                    }
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.Adjust)?.copy(devices = devices) ?: state
                    }
                    navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it))
                }
        }
    }

    // 이 화면(Adjust 목록/추가)은 SmartThings 계정 연동 기반 추가 플로우(AdminSmartThingsAddScreen)로
    // 대체되어 더 이상 실제 진입 동선이 없다 - place/name 로컬 입력폼도 실 등록 API가 없어 로컬에만
    // 추가되는 장식용 동작을 그대로 유지(범위 밖, #181)
    private fun addDevice(place: String, name: String) {
        val newDevice = AdminAdjustUiState.DeviceItem(
            id = "local-${devices.size + 1}",
            place = place,
            name = name,
            isOnline = true,
            isPowerOn = false
        )
        devices = devices + newDevice
        navController.clearAndReset(AdminAdjustUiState.Adjust(devices))
    }

    fun handleTabClick(tab: AdminAdjustUiState.ControlMethod) {
        navController.updateCurrentState { state ->
            if (state is AdminAdjustUiState.DeviceDetail) {
                state.copy(selectedTab = tab)
            } else state
        }

        loadData(tab)
    }

    private fun loadData(tab: AdminAdjustUiState.ControlMethod? = null) {
        if (tab != AdminAdjustUiState.ControlMethod.MANUALEdit) return
        val current = navController.uiState.value as? AdminAdjustUiState.DeviceDetail ?: return

        viewModelScope.launch {
            getAutoControlRuleUseCase(current.deviceId)
                .onSuccess { rule ->
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.DeviceDetail)
                            ?.copy(
                                autoControlState = rule?.toUiState()
                                    ?: defaultAutoControlState(current.device.deviceType)
                            )
                            ?: state
                    }
                }
                .onFailure { navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it)) }
        }
    }

    private fun sendRemoteControl(intent: AdminAdjustIntent.SendRemoteControl) {
        val target = devices.find { it.id == intent.deviceId } ?: return

        launchControl(intent.deviceId) {
            controlDeviceUseCase(
                deviceId = target.id,
                isPowerOn = intent.isPowerOn,
                operationMode = intent.operationMode?.name,
                windSpeed = intent.windSpeed?.name,
                temperature = intent.temperature?.toInt(),
            )
                .onSuccess { navController.emitEvent(AdminAdjustEvent.ShowSnackbar("제어 명령을 전송했습니다.")) }
                .onFailure { navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it)) }
        }
    }

    private fun sendAutoControl(intent: AdminAdjustIntent.SendAutoControl) {
        viewModelScope.launch {
            saveAutoControlRuleUseCase(intent.deviceId, intent.autoControlState.toDomain())
                .onSuccess { saved ->
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.DeviceDetail)?.copy(autoControlState = saved.toUiState()) ?: state
                    }
                    navController.emitEvent(AdminAdjustEvent.ShowSnackbar("자동 제어 규칙을 저장했습니다."))
                }
                .onFailure { navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it)) }
        }
    }

    private fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            deleteDeviceUseCase(deviceId)
                .onSuccess {
                    devices = devices.filterNot { it.id == deviceId }
                    // navController.navigateBack()을 직접 부르면 내부 목록 화면(Adjust)을 거치게
                    // 된다 - 기기 상세에서 나갈 때는 항상 Home으로 바로 나가야 하므로(#235) 그
                    // 규칙이 이미 구현된 private navigateBack()을 대신 호출한다.
                    navigateBack()
                    navController.updateCurrentState { state ->
                        (state as? AdminAdjustUiState.Adjust)?.copy(devices = devices) ?: state
                    }
                    navController.emitEvent(AdminAdjustEvent.ShowSnackbar("기기를 해제했어요."))
                }
                .onFailure {
                    navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it))
                }
        }
    }
}

// 서버에 저장되지 않은 필드는 화면의 프리셋 기본값으로 채운다(#190) - 신규 기기는 규칙이 아예 없어
// 응답 필드 전부가 null일 수 있음.
private fun AutoControlRule.toUiState(): AutoControlUiState {
    val default = AutoControlUiState()
    return AutoControlUiState(
        discomfortThreshold = discomfortThreshold ?: default.discomfortThreshold,
        discomfortAction = discomfortAction ?: default.discomfortAction,
        humidityHighThreshold = humidityHighThreshold ?: default.humidityHighThreshold,
        humidityHighAction = humidityHighAction ?: default.humidityHighAction,
        humidityLowThreshold = humidityLowThreshold ?: default.humidityLowThreshold,
        humidityLowAction = humidityLowAction ?: default.humidityLowAction,
        temperatureHighThreshold = temperatureHighThreshold ?: default.temperatureHighThreshold,
        temperatureHighAction = temperatureHighAction ?: default.temperatureHighAction,
        temperatureLowThreshold = temperatureLowThreshold ?: default.temperatureLowThreshold,
        temperatureLowAction = temperatureLowAction ?: default.temperatureLowAction,
        occupancyThreshold = occupancyThreshold ?: default.occupancyThreshold,
        occupancyAction = occupancyAction ?: default.occupancyAction,
        isAutoOffEnabled = isAutoOffEnabled,
        autoOffMinutes = autoOffMinutes ?: default.autoOffMinutes,
    )
}

private fun AutoControlUiState.toDomain(): AutoControlRule =
    AutoControlRule(
        discomfortThreshold = discomfortThreshold,
        discomfortAction = discomfortAction,
        humidityHighThreshold = humidityHighThreshold,
        humidityHighAction = humidityHighAction,
        humidityLowThreshold = humidityLowThreshold,
        humidityLowAction = humidityLowAction,
        temperatureHighThreshold = temperatureHighThreshold,
        temperatureHighAction = temperatureHighAction,
        temperatureLowThreshold = temperatureLowThreshold,
        temperatureLowAction = temperatureLowAction,
        occupancyThreshold = occupancyThreshold,
        occupancyAction = occupancyAction,
        isAutoOffEnabled = isAutoOffEnabled,
        autoOffMinutes = autoOffMinutes,
    )
