package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.home.component.IoTDeviceItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    // 기기 상세(설정 > IoT 기기 목록)는 "지금 선택된 룸"만 보여주는 화면이 아니라 관리자가 가진
    // 모든 룸의 기기를 한 번에 보여줘야 해서(#166, 피그마 요구사항) 첫 장소로 고정하던 이전 로직을
    // 버리고 장소마다 병렬로 기기 목록을 조회한다. 룸 수가 적은 가정용 앱이라 동시 조회 수를
    // 별도로 제한하지 않는다(AdminSettingViewModel의 관리자 조회처럼 무거운 호출도 아님).
    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = AdminIotDeviceListUiState.Loading
            val places = getPlaceListUseCase().getOrNull().orEmpty()
            if (places.isEmpty()) {
                _event.emit(AdminIotDeviceListEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
                _uiState.value = AdminIotDeviceListUiState.Loaded(emptyList())
                return@launch
            }

            val results = coroutineScope {
                places.map { place -> async { place to getDeviceListUseCase(place.placeId) } }.awaitAll()
            }

            val failure = results.firstNotNullOfOrNull { (_, result) -> result.exceptionOrNull() }
            if (failure != null) {
                _event.emit(AdminIotDeviceListEvent.ShowErrorSnackbar(failure))
            }

            devices = results.flatMap { (place, result) ->
                result.getOrNull().orEmpty().map { device ->
                    IoTDeviceItem(
                        id = device.deviceId,
                        place = place.name,
                        name = device.deviceName ?: device.deviceId,
                        isOnline = device.isOnline,
                        deviceType = resolveDeviceType(device.deviceType, device.category),
                        isPowerOn = false
                    )
                }
            }
            _uiState.value = AdminIotDeviceListUiState.Loaded(devices)
        }
    }

    // ARDUINO/AI_SPEAKER/AEROMETER는 category가 항상 null이라 deviceType으로 직접 분기하고(#298),
    // 그 외(SmartThings)는 등록 시 관리자가 고른 category로 아이콘을 정한다.
    private fun resolveDeviceType(serverDeviceType: String, category: String?): DeviceType = when (serverDeviceType) {
        "ARDUINO" -> DeviceType.ARDUINO
        "AI_SPEAKER" -> DeviceType.AI_SPEAKER
        "AEROMETER" -> DeviceType.AEROMETER
        else -> category?.let { runCatching { DeviceType.valueOf(it) }.getOrNull() } ?: DeviceType.OTHER
    }

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
