package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.SmartThingsCallbackSignal
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.GetSmartThingsAuthorizeUrlUseCase
import com.reborn.core.domain.usecase.GetSmartThingsDeviceListUseCase
import com.reborn.core.domain.usecase.RegisterSmartThingsDeviceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmartThingsDeviceItem(val deviceId: String, val label: String)

sealed interface AdminSmartThingsAddUiState {
    data object Idle : AdminSmartThingsAddUiState
    data object Loading : AdminSmartThingsAddUiState
    data object AwaitingConsent : AdminSmartThingsAddUiState
    data class DeviceList(val devices: List<SmartThingsDeviceItem>) : AdminSmartThingsAddUiState
    data class DeviceNaming(
        val device: SmartThingsDeviceItem,
        val name: String,
        val category: String? = null,
    ) : AdminSmartThingsAddUiState
}

sealed class AdminSmartThingsAddEvent {
    data class OpenUrl(val url: String) : AdminSmartThingsAddEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminSmartThingsAddEvent()
    data object RegisterSuccess : AdminSmartThingsAddEvent()
}

class AdminSmartThingsAddViewModel(
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getSmartThingsAuthorizeUrlUseCase: GetSmartThingsAuthorizeUrlUseCase,
    private val getSmartThingsDeviceListUseCase: GetSmartThingsDeviceListUseCase,
    private val registerSmartThingsDeviceUseCase: RegisterSmartThingsDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminSmartThingsAddUiState>(AdminSmartThingsAddUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminSmartThingsAddEvent>()
    val event = _event.asSharedFlow()

    init {
        // SmartThings 동의 완료 후 서버 콜백 페이지가 딥링크로 앱을 다시 열면(#239) 사람이
        // "돌아와서 눌러주세요" 버튼을 누르지 않아도 자동으로 연동된 기기 목록을 불러온다.
        // 이 화면이 AwaitingConsent 상태로 대기 중일 때만 반응 - 다른 단계(기기 이름 입력 중 등)에서
        // 뒤늦게 신호가 와도 사용자가 입력하던 걸 덮어쓰지 않게 한다.
        viewModelScope.launch {
            SmartThingsCallbackSignal.events.collect { success ->
                if (_uiState.value != AdminSmartThingsAddUiState.AwaitingConsent) return@collect
                if (success) {
                    loadDevices()
                } else {
                    _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(IllegalStateException("SmartThings 연동에 실패했습니다.")))
                }
            }
        }
    }

    // TODO: 장소 선택/전환 개념이 앱에 아직 없어(#166 참고) 첫 번째 장소로 임시 고정한다.
    // 다중 장소를 관리하는 관리자가 늘어나면 장소 선택 UI를 별도로 추가해야 함.
    private var placeId: Long? = null

    private suspend fun resolvePlaceId(): Long? {
        placeId?.let { return it }
        val resolved = getPlaceListUseCase().getOrNull()?.firstOrNull()?.placeId
        placeId = resolved
        return resolved
    }

    fun startAuthorize() {
        viewModelScope.launch {
            _uiState.value = AdminSmartThingsAddUiState.Loading
            val pid = resolvePlaceId()
            if (pid == null) {
                _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
                _uiState.value = AdminSmartThingsAddUiState.Idle
                return@launch
            }
            getSmartThingsAuthorizeUrlUseCase(pid)
                .onSuccess { url ->
                    _event.emit(AdminSmartThingsAddEvent.OpenUrl(url))
                    _uiState.value = AdminSmartThingsAddUiState.AwaitingConsent
                }
                .onFailure {
                    _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(it))
                    _uiState.value = AdminSmartThingsAddUiState.Idle
                }
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = AdminSmartThingsAddUiState.Loading
            val pid = resolvePlaceId()
            if (pid == null) {
                _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
                _uiState.value = AdminSmartThingsAddUiState.Idle
                return@launch
            }
            getSmartThingsDeviceListUseCase(pid)
                .onSuccess { devices ->
                    _uiState.value = AdminSmartThingsAddUiState.DeviceList(
                        devices.map { SmartThingsDeviceItem(it.deviceId, it.label ?: it.deviceId) }
                    )
                }
                .onFailure {
                    // SmartThings 연동이 아직 안 됐거나 만료된 경우 등 - 다시 연동 유도
                    _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(it))
                    _uiState.value = AdminSmartThingsAddUiState.AwaitingConsent
                }
        }
    }

    fun selectDevice(device: SmartThingsDeviceItem) {
        _uiState.value = AdminSmartThingsAddUiState.DeviceNaming(device, device.label)
    }

    fun backToDeviceList() {
        loadDevices()
    }

    fun updateDeviceName(name: String) {
        val current = _uiState.value
        if (current is AdminSmartThingsAddUiState.DeviceNaming) {
            _uiState.value = current.copy(name = name)
        }
    }

    fun updateDeviceCategory(category: String) {
        val current = _uiState.value
        if (current is AdminSmartThingsAddUiState.DeviceNaming) {
            _uiState.value = current.copy(category = category)
        }
    }

    fun registerDevice() {
        val current = _uiState.value
        if (current !is AdminSmartThingsAddUiState.DeviceNaming) return
        val pid = placeId
        if (pid == null) {
            viewModelScope.launch {
                _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(IllegalStateException("등록된 장소가 없습니다.")))
            }
            return
        }

        _uiState.value = AdminSmartThingsAddUiState.Loading
        viewModelScope.launch {
            registerSmartThingsDeviceUseCase(pid, current.device.deviceId, current.name, current.category)
                .onSuccess { _event.emit(AdminSmartThingsAddEvent.RegisterSuccess) }
                .onFailure {
                    _event.emit(AdminSmartThingsAddEvent.ShowErrorSnackbar(it))
                    _uiState.value = current
                }
        }
    }
}
