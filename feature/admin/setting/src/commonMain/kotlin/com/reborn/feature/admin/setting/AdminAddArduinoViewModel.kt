package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.RegisterArduinoDeviceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminAddArduinoUiState {
    data object Idle : AdminAddArduinoUiState
    data object Submitting : AdminAddArduinoUiState
}

sealed class AdminAddArduinoEvent {
    data object RegisterSuccess : AdminAddArduinoEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAddArduinoEvent()
}

class AdminAddArduinoViewModel(
    private val registerArduinoDeviceUseCase: RegisterArduinoDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAddArduinoUiState>(AdminAddArduinoUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminAddArduinoEvent>()
    val event = _event.asSharedFlow()

    fun register(placeId: Long, deviceId: String, deviceName: String) {
        if (_uiState.value == AdminAddArduinoUiState.Submitting) return

        viewModelScope.launch {
            _uiState.value = AdminAddArduinoUiState.Submitting
            registerArduinoDeviceUseCase(placeId, deviceId, deviceName)
                .onSuccess { _event.emit(AdminAddArduinoEvent.RegisterSuccess) }
                .onFailure { _event.emit(AdminAddArduinoEvent.ShowErrorSnackbar(it)) }
            _uiState.value = AdminAddArduinoUiState.Idle
        }
    }
}
