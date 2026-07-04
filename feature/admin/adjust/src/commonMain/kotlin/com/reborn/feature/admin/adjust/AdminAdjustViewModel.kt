package com.reborn.feature.admin.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.NavigationManager
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AdminAdjustEvent {
    data object Exit : AdminAdjustEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAdjustEvent()
    data class ShowSnackbar(val message: String) : AdminAdjustEvent()
}

class AdminAdjustViewModel : ViewModel() {
    private val navController = NavigationManager<AdminAdjustUiState, AdminAdjustEvent>(
        initialState = AdminAdjustUiState.Loading,
        exitEvent = AdminAdjustEvent.Exit,
        scope = viewModelScope
    )

    val uiState = navController.uiState
    val event = navController.event

    // TODO: 서버 device API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정
    private var devices: List<AdminAdjustUiState.DeviceItem> = listOf(
        AdminAdjustUiState.DeviceItem(1, "거실", "거실 조명", isOnline = true, isPowerOn = true, deviceType = DeviceType.LAMP),
        AdminAdjustUiState.DeviceItem(2, "거실", "거실 공기청정기", isOnline = true, isPowerOn = false, deviceType = DeviceType.AIR_CONDITIONER),
        AdminAdjustUiState.DeviceItem(3, "안방", "안방 가습기", isOnline = false, isPowerOn = false, deviceType = DeviceType.OTHER)
    )

    fun onIntent(intent: AdminAdjustIntent) {
        when (intent) {
            is AdminAdjustIntent.LoadInitial -> checkInitialState()
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

    private fun checkInitialState() {
        navController.clearAndReset(AdminAdjustUiState.Loading)
        viewModelScope.launch {
            delay(1500)
            navController.clearAndReset(AdminAdjustUiState.Adjust(devices))
        }
    }

    private fun navigateToDeviceDetail(intent: AdminAdjustIntent.NavigateToDeviceDetail) {
        val device = devices.find { it.id == intent.deviceId } ?: return
        navController.navigateTo(AdminAdjustUiState.DeviceDetail(intent.controlMethod, intent.deviceId, device))
    }

    private fun togglePower(deviceId: Int) {
        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = !device.isPowerOn) else device
        }
        navController.updateCurrentState { state ->
            (state as? AdminAdjustUiState.Adjust)?.copy(devices = devices) ?: state
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

    // TODO: 서버 기기 제어 명령 API 연동 전까지의 목업. 실제 연동 시 UseCase/Repository로 대체 예정
    private fun sendRemoteControl(intent: AdminAdjustIntent.SendRemoteControl) {
        viewModelScope.launch {
            delay(500)
            navController.emitEvent(AdminAdjustEvent.ShowSnackbar("제어 명령을 전송했습니다."))
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
