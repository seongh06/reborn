package com.reborn.feature.admin.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AdminAdjustEvent {
    data object Exit : AdminAdjustEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAdjustEvent()
    data class ShowSnackbar(val message: String) : AdminAdjustEvent()
}

class AdminAdjustViewModel(
    private val controlDeviceUseCase: ControlDeviceUseCase
) : ViewModel() {
    private val navController = NavigationManager<AdminAdjustUiState, AdminAdjustEvent>(
        initialState = AdminAdjustUiState.Loading,
        exitEvent = AdminAdjustEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    // TODO: 서버 device 목록 API(GetDeviceListUseCase) 연동 전까지의 목업 데이터 - place(방)는 서버
    // 도메인에 아예 없는 개념(#166)이고, 목록 API는 파워 상태도 안 내려줘서(제어 API로만 변경 가능) 이
    // mock을 실 데이터로 옮기려면 그 두 gap을 먼저 메워야 함, 이번 이슈(#134)는 제어 명령 전송만 실연동
    private var devices: List<AdminAdjustUiState.DeviceItem> = listOf(
        AdminAdjustUiState.DeviceItem(
            id = 1, place = "거실", name = "거실 조명",
            isOnline = true, isPowerOn = true,
            deviceType = DeviceType.LAMP, deviceKey = "mock-lamp-01"
        ),
        AdminAdjustUiState.DeviceItem(
            id = 2, place = "거실", name = "거실 공기청정기",
            isOnline = true, isPowerOn = false,
            deviceType = DeviceType.AIR_CONDITIONER, deviceKey = "mock-ac-01"
        ),
        AdminAdjustUiState.DeviceItem(
            id = 3, place = "안방", name = "안방 가습기",
            isOnline = false, isPowerOn = false,
            deviceType = DeviceType.OTHER, deviceKey = "mock-humidifier-01"
        )
    )

    // 동일 기기에 토글/원격 제어가 겹쳐 들어오면 이전 요청의 응답이 나중에 도착해 UI가
    // 실제 상태와 어긋날 수 있어(CodeRabbit #178) 기기별로 진행 중인 제어 요청을 직렬화한다.
    private val pendingControlJobs = mutableMapOf<Int, Job>()

    private fun launchControl(deviceId: Int, block: suspend () -> Unit) {
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
        }
    }

    private fun checkInitialState(deviceId: Int? = null) {
        navController.clearAndReset(AdminAdjustUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            navController.clearAndReset(AdminAdjustUiState.Adjust(devices))
            if (deviceId != null) {
                navigateToDeviceDetail(AdminAdjustIntent.NavigateToDeviceDetail(deviceId = deviceId))
            }
        }
    }

    private fun navigateToDeviceDetail(intent: AdminAdjustIntent.NavigateToDeviceDetail) {
        val device = devices.find { it.id == intent.deviceId } ?: return
        navController.navigateTo(AdminAdjustUiState.DeviceDetail(intent.controlMethod, intent.deviceId, device))
    }

    private fun togglePower(deviceId: Int) {
        val target = devices.find { it.id == deviceId } ?: return
        if (target.deviceKey.isBlank()) {
            navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(IllegalStateException("등록되지 않은 기기입니다.")))
            return
        }
        val nextPowerOn = !target.isPowerOn

        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = nextPowerOn) else device
        }
        navController.updateCurrentState { state ->
            (state as? AdminAdjustUiState.Adjust)?.copy(devices = devices) ?: state
        }

        launchControl(deviceId) {
            controlDeviceUseCase(deviceId = target.deviceKey, isPowerOn = nextPowerOn)
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

    private fun addDevice(place: String, name: String) {
        val newDevice = AdminAdjustUiState.DeviceItem(
            id = (devices.maxOfOrNull { it.id } ?: 0) + 1,
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

    private fun loadData(
        tab: AdminAdjustUiState.ControlMethod?=null
    ){

    }

    private fun sendRemoteControl(intent: AdminAdjustIntent.SendRemoteControl) {
        val target = devices.find { it.id == intent.deviceId } ?: return
        if (target.deviceKey.isBlank()) {
            navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(IllegalStateException("등록되지 않은 기기입니다.")))
            return
        }

        launchControl(intent.deviceId) {
            controlDeviceUseCase(
                deviceId = target.deviceKey,
                isPowerOn = intent.isPowerOn,
                operationMode = intent.operationMode.name,
                windSpeed = intent.windSpeed.name,
                temperature = intent.temperature.toInt(),
            )
                .onSuccess { navController.emitEvent(AdminAdjustEvent.ShowSnackbar("제어 명령을 전송했습니다.")) }
                .onFailure { navController.emitEvent(AdminAdjustEvent.ShowErrorSnackbar(it)) }
        }
    }

    // TODO: 서버 자동제어 규칙 API 연동 전까지의 목업. 실제 연동 시 UseCase/Repository로 대체 예정
    private fun sendAutoControl(intent: AdminAdjustIntent.SendAutoControl) {
        viewModelScope.launch {
            delay(500)
            navController.emitEvent(AdminAdjustEvent.ShowSnackbar("자동 제어 규칙을 저장했습니다."))
        }
    }
}
