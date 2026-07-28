package com.reborn.feature.admin.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.DeleteDeviceUseCase
import com.reborn.core.domain.usecase.GetAutoControlRuleUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.SaveAutoControlRuleUseCase
import com.reborn.core.model.AutoControlRule
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class AdminAdjustEvent {
    data object Exit : AdminAdjustEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAdjustEvent()
    data class ShowSnackbar(val message: String) : AdminAdjustEvent()
}

class AdminAdjustViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val controlDeviceUseCase: ControlDeviceUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase,
    private val saveAutoControlRuleUseCase: SaveAutoControlRuleUseCase,
    private val getAutoControlRuleUseCase: GetAutoControlRuleUseCase
) : ViewModel() {
    private val navController = NavigationManager<AdminAdjustUiState, AdminAdjustEvent>(
        initialState = AdminAdjustUiState.Loading,
        exitEvent = AdminAdjustEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    private var devices: List<AdminAdjustUiState.DeviceItem> = emptyList()

    // TODO: 장소 선택/전환 개념이 앱에 아직 없어(#166 참고) 첫 번째 장소로 임시 고정한다.
    private var resolvedPlaceId: Long? = null

    private suspend fun resolvePlaceId(): Long? {
        resolvedPlaceId?.let { return it }
        val resolved = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        resolvedPlaceId = resolved
        return resolved
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
            is AdminAdjustIntent.NavigateBack -> navController.navigateBack()
            is AdminAdjustIntent.NavigateToAddDevice -> navController.navigateTo(AdminAdjustUiState.AddDevice)
            is AdminAdjustIntent.NavigateToDeviceDetail -> navigateToDeviceDetail(intent)
            is AdminAdjustIntent.TogglePower -> togglePower(intent.deviceId)
            is AdminAdjustIntent.AddDevice -> addDevice(intent.place, intent.name)
            is AdminAdjustIntent.ClickTab -> handleTabClick(intent.tab)
            is AdminAdjustIntent.SendRemoteControl -> sendRemoteControl(intent)
            is AdminAdjustIntent.SendAutoControl -> sendAutoControl(intent)
            is AdminAdjustIntent.DeleteDevice -> deleteDevice(intent.deviceId)
        }
    }

    private fun checkInitialState(deviceId: String? = null) {
        navController.clearAndReset(AdminAdjustUiState.Loading)
        viewModelScope.launch {
            val placeId = resolvePlaceId()
            if (placeId == null) {
                navController.emitEvent(
                    AdminAdjustEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다."))
                )
                navController.clearAndReset(AdminAdjustUiState.Adjust(emptyList()))
                return@launch
            }

            getDeviceListUseCase(placeId)
                .onSuccess { list ->
                    devices = list.map { device ->
                        AdminAdjustUiState.DeviceItem(
                            id = device.deviceId,
                            place = deviceTypeLabel(device.deviceType),
                            name = device.deviceName ?: device.deviceId,
                            isOnline = device.isOnline
                        )
                    }
                    navController.clearAndReset(AdminAdjustUiState.Adjust(devices))
                    if (deviceId != null) {
                        navigateToDeviceDetail(AdminAdjustIntent.NavigateToDeviceDetail(deviceId = deviceId))
                    }
                }
                .onFailure {
                    navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it))
                    navController.clearAndReset(AdminAdjustUiState.Adjust(emptyList()))
                }
        }
    }

    // 서버 device 도메인에 방(room) 개념이 없어(#166) 대신 기기 종류로 그룹/부제목을 표시
    private fun deviceTypeLabel(serverDeviceType: String): String = when (serverDeviceType) {
        "ARDUINO" -> "아두이노"
        "SMART_THINGS" -> "SmartThings"
        "AI_SPEAKER" -> "AI 스피커"
        else -> serverDeviceType
    }

    private fun navigateToDeviceDetail(intent: AdminAdjustIntent.NavigateToDeviceDetail) {
        val device = devices.find { it.id == intent.deviceId }
            ?: return navController.emitEvent(
                AdminAdjustEvent.ShowErrorSnackbar(IllegalArgumentException("기기를 찾을 수 없습니다."))
            )
        navController.navigateTo(AdminAdjustUiState.DeviceDetail(intent.controlMethod, intent.deviceId, device))
        loadData(intent.controlMethod)
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
                            ?.copy(autoControlState = rule?.toUiState() ?: AutoControlUiState())
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
                operationMode = intent.operationMode.name,
                windSpeed = intent.windSpeed.name,
                temperature = intent.temperature.toInt(),
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
                    navController.navigateBack()
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
