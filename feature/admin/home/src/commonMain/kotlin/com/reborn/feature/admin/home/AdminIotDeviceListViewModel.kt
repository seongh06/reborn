package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.home.component.IoTDeviceItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminIotDeviceListUiState {
    data object Loading : AdminIotDeviceListUiState
    data class Loaded(val devices: List<IoTDeviceItem> = emptyList()) : AdminIotDeviceListUiState
}

sealed class AdminIotDeviceListEvent {
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminIotDeviceListEvent()
}

class AdminIotDeviceListViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getDeviceListUseCase: GetDeviceListUseCase,
    private val controlDeviceUseCase: ControlDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminIotDeviceListUiState>(AdminIotDeviceListUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminIotDeviceListEvent>()
    val event = _event.asSharedFlow()

    private var devices: List<IoTDeviceItem> = emptyList()

    // TODO: 장소 선택/전환 개념이 앱에 아직 없어(#166 참고) 첫 번째 장소로 임시 고정한다.
    private var placeId: Long? = null

    private suspend fun resolvePlaceId(): Long? {
        placeId?.let { return it }
        val resolved = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        placeId = resolved
        return resolved
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = AdminIotDeviceListUiState.Loading
            val pid = resolvePlaceId()
            if (pid == null) {
                _event.emit(AdminIotDeviceListEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
                _uiState.value = AdminIotDeviceListUiState.Loaded(emptyList())
                return@launch
            }

            getDeviceListUseCase(pid)
                .onSuccess { list ->
                    devices = list.map { device ->
                        IoTDeviceItem(
                            id = device.deviceId,
                            place = deviceTypeLabel(device.deviceType),
                            name = device.deviceName ?: device.deviceId,
                            isOnline = device.isOnline,
                            deviceType = categoryToDeviceType(device.category),
                            isPowerOn = false
                        )
                    }
                    _uiState.value = AdminIotDeviceListUiState.Loaded(devices)
                }
                .onFailure {
                    _event.emit(AdminIotDeviceListEvent.ShowErrorSnackbar(it))
                    _uiState.value = AdminIotDeviceListUiState.Loaded(emptyList())
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

    // SmartThings 기기만 category(아이콘 구분용)가 있을 수 있고, 나머지는 null이라 항상 OTHER
    private fun categoryToDeviceType(category: String?): DeviceType =
        category?.let { runCatching { DeviceType.valueOf(it) }.getOrNull() } ?: DeviceType.OTHER

    fun togglePower(deviceId: String) {
        val target = devices.find { it.id == deviceId } ?: return
        val nextPowerOn = !target.isPowerOn

        devices = devices.map { device ->
            if (device.id == deviceId) device.copy(isPowerOn = nextPowerOn) else device
        }
        _uiState.value = AdminIotDeviceListUiState.Loaded(devices)

        viewModelScope.launch {
            controlDeviceUseCase(deviceId = deviceId, isPowerOn = nextPowerOn)
                .onFailure {
                    devices = devices.map { device ->
                        if (device.id == deviceId) device.copy(isPowerOn = target.isPowerOn) else device
                    }
                    _uiState.value = AdminIotDeviceListUiState.Loaded(devices)
                    _event.emit(AdminIotDeviceListEvent.ShowErrorSnackbar(it))
                }
        }
    }
}
