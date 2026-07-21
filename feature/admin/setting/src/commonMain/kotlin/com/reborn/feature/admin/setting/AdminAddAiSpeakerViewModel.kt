package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.domain.usecase.RegisterAiSpeakerDeviceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdminAddAiSpeakerUiState {
    data object Idle : AdminAddAiSpeakerUiState
    data object Submitting : AdminAddAiSpeakerUiState
}

sealed class AdminAddAiSpeakerEvent {
    data object RegisterSuccess : AdminAddAiSpeakerEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : AdminAddAiSpeakerEvent()
}

class AdminAddAiSpeakerViewModel(
    private val registerAiSpeakerDeviceUseCase: RegisterAiSpeakerDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminAddAiSpeakerUiState>(AdminAddAiSpeakerUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminAddAiSpeakerEvent>()
    val event = _event.asSharedFlow()

    fun register(placeId: Long, deviceId: String, deviceName: String) {
        if (_uiState.value == AdminAddAiSpeakerUiState.Submitting) return

        viewModelScope.launch {
            _uiState.value = AdminAddAiSpeakerUiState.Submitting
            registerAiSpeakerDeviceUseCase(placeId, deviceId, deviceName)
                .onSuccess { _event.emit(AdminAddAiSpeakerEvent.RegisterSuccess) }
                .onFailure { _event.emit(AdminAddAiSpeakerEvent.ShowErrorSnackbar(it)) }
            _uiState.value = AdminAddAiSpeakerUiState.Idle
        }
    }
}
