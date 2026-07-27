package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.ConfigureDeviceWifiUseCase
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
}

class AdminDeviceWifiSetupViewModel(
    private val configureDeviceWifiUseCase: ConfigureDeviceWifiUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDeviceWifiSetupUiState>(AdminDeviceWifiSetupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminDeviceWifiSetupEvent>()
    val event = _event.asSharedFlow()

    fun configure(ssid: String, password: String, deviceId: String) {
        if (_uiState.value == AdminDeviceWifiSetupUiState.Submitting) return

        viewModelScope.launch {
            _uiState.value = AdminDeviceWifiSetupUiState.Submitting
            configureDeviceWifiUseCase(ssid, password, deviceId)
                .onSuccess { _event.emit(AdminDeviceWifiSetupEvent.ConfigureSuccess) }
                .onFailure { _event.emit(AdminDeviceWifiSetupEvent.ShowErrorSnackbar(it)) }
            _uiState.value = AdminDeviceWifiSetupUiState.Idle
        }
    }
}
