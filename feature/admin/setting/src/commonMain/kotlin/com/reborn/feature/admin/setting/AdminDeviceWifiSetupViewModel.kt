package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.ConfigureDeviceWifiUseCase
import com.reborn.core.domain.usecase.GetPlaceWifiUseCase
import com.reborn.core.domain.usecase.UpdatePlaceWifiUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminDeviceWifiSetupUiState {
    data object Idle : AdminDeviceWifiSetupUiState
    data object Submitting : AdminDeviceWifiSetupUiState
}

sealed class AdminDeviceWifiSetupEvent {
    data object ConfigureSuccess : AdminDeviceWifiSetupEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminDeviceWifiSetupEvent()
    // 이 장소에 저장된 WiFi가 있으면 입력 필드를 자동으로 채워 관리자가 다시 타이핑하지 않게 한다(#219).
    // 저장된 적 없으면 ssid=null이라 화면에서 빈 입력 상태를 그대로 유지.
    data class SavedWifiLoaded(val ssid: String?, val password: String?) : AdminDeviceWifiSetupEvent()
}

class AdminDeviceWifiSetupViewModel(
    private val configureDeviceWifiUseCase: ConfigureDeviceWifiUseCase,
    private val getPlaceWifiUseCase: GetPlaceWifiUseCase,
    private val updatePlaceWifiUseCase: UpdatePlaceWifiUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDeviceWifiSetupUiState>(AdminDeviceWifiSetupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminDeviceWifiSetupEvent>()
    val event = _event.asSharedFlow()

    fun loadSavedWifi(placeId: Long) {
        viewModelScope.launch {
            getPlaceWifiUseCase(placeId)
                .onSuccess { _event.emit(AdminDeviceWifiSetupEvent.SavedWifiLoaded(it.ssid, it.password)) }
            // 조회 실패는 조용히 무시 - 자동 채움이 안 될 뿐, 수동 입력으로 계속 진행 가능
        }
    }

    fun configure(ssid: String, password: String, deviceId: String, placeId: Long) {
        if (_uiState.value == AdminDeviceWifiSetupUiState.Submitting) return

        viewModelScope.launch {
            _uiState.value = AdminDeviceWifiSetupUiState.Submitting
            configureDeviceWifiUseCase(ssid, password, deviceId)
                .onSuccess {
                    // 이 장소의 다음 기기부터는 재입력 없이 재사용되도록 저장 - 실패해도(네트워크 등)
                    // 방금 기기 설정 자체는 이미 성공했으니 사용자에게 실패로 보이지 않게 조용히 무시
                    updatePlaceWifiUseCase(placeId, ssid, password)
                    _event.emit(AdminDeviceWifiSetupEvent.ConfigureSuccess)
                }
                .onFailure { _event.emit(AdminDeviceWifiSetupEvent.ShowErrorSnackbar(it)) }
            _uiState.value = AdminDeviceWifiSetupUiState.Idle
        }
    }
}
