package com.reborn.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IntroEvent {
    data object NavigateToAdmin : IntroEvent()
    data object NavigateToAerometer : IntroEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : IntroEvent()
}

class IntroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<IntroUiState>(IntroUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<IntroEvent>()
    val event = _event.asSharedFlow()

    fun onIntent(intent: IntroIntent){
        when(intent){
            is IntroIntent.LoadInitial -> {}
            is IntroIntent.NavigateToAdmin -> navigateToAdmin()
            is IntroIntent.NavigateToAerometer -> navigateToAerometer()
        }
    }

    private fun navigateToAdmin() {
        viewModelScope.launch {
            _event.emit(IntroEvent.NavigateToAdmin)
        }
    }

    private fun navigateToAerometer() {
        viewModelScope.launch {
            _event.emit(IntroEvent.NavigateToAerometer)
        }
    }

}
