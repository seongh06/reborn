package com.reborn.feature.aerometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.SensorAnalyzer
import com.reborn.feature.aerometer.model.AerometerIntent
import com.reborn.feature.aerometer.model.AerometerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AerometerEvent {
    data class ShowErrorSnackbar(val throwable: Throwable) : AerometerEvent()
    data class ShowSensorResult(val personCount: Int, val lux: Int) : AerometerEvent()
    data class ShowImageSaved(val path: String) : AerometerEvent()
    data object Exit : AerometerEvent()
}

class AerometerViewModel(private val sensorAnalyzer: SensorAnalyzer) : ViewModel() {
    private val _uiState = MutableStateFlow<AerometerUiState>(AerometerUiState.Loading)
    val uiState: StateFlow<AerometerUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AerometerEvent>()
    val event = _event.asSharedFlow()

    private val _isSaveImageEnabled = MutableStateFlow(false)
    val isSaveImageEnabled: StateFlow<Boolean> = _isSaveImageEnabled.asStateFlow()

    private val backStack = mutableListOf<AerometerUiState>()

    fun onIntent(intent: AerometerIntent) {
        when (intent) {
            is AerometerIntent.LoadInitial -> checkInitialState()
            is AerometerIntent.NavigateToSetting -> navigateTo(AerometerUiState.Setting)
            is AerometerIntent.NavigateBack -> navigateBack()
            is AerometerIntent.ToggleSaveImage -> _isSaveImageEnabled.update { !it }
        }
    }

    private fun checkInitialState() {
        backStack.clear()
        _uiState.value = AerometerUiState.Loading
        viewModelScope.launch {
            delay(1500)
            _uiState.value = AerometerUiState.Home
            startPeriodicScan()
        }
    }

    private fun startPeriodicScan() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                try {
                    val result = sensorAnalyzer.analyze(saveImage = _isSaveImageEnabled.value)
                    _event.emit(AerometerEvent.ShowSensorResult(result.personCount, result.lux))
                    result.savedImagePath?.let { path ->
                        _event.emit(AerometerEvent.ShowImageSaved(path))
                    }
                } catch (e: Exception) {
                    _event.emit(AerometerEvent.ShowErrorSnackbar(e))
                }
            }
        }
    }

    private fun navigateTo(next: AerometerUiState) {
        if (_uiState.value == next) return
        backStack.add(_uiState.value)
        _uiState.value = next
    }

    private fun navigateBack() {
        val previous = backStack.removeLastOrNull()
        if (previous != null) {
            _uiState.value = previous
        } else {
            viewModelScope.launch {
                _event.emit(AerometerEvent.Exit)
            }
        }
    }
}
