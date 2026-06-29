package com.reborn.feature.aerometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.feature.aerometer.model.AerometerIntent
import com.reborn.feature.aerometer.model.AerometerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AerometerEvent {
    data class ShowErrorSnackbar(val throwable: Throwable) : AerometerEvent()
    data object Exit : AerometerEvent()
}

class AerometerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AerometerUiState>(AerometerUiState.Loading)
    val uiState: StateFlow<AerometerUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AerometerEvent>()
    val event = _event.asSharedFlow()

    private val backStack = mutableListOf<AerometerUiState>()

    fun onIntent(intent: AerometerIntent){
        when(intent){
            is AerometerIntent.LoadInitial -> checkInitialState()
            is AerometerIntent.NavigateToSetting -> navigateTo(AerometerUiState.Setting)
            is AerometerIntent.NavigateBack -> navigateBack()
        }
    }

    private fun checkInitialState() {
        backStack.clear()
        _uiState.value = AerometerUiState.Loading
        viewModelScope.launch {
            delay(1500)
            _uiState.value = AerometerUiState.Home
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
            // 더 돌아갈 내부 화면이 없으면 Intro 자체를 빠져나가는 건 호출부(outer onBackClick)에 맡긴다.
            viewModelScope.launch {
                _event.emit(AerometerEvent.Exit)
            }
        }
    }
}
