package com.reborn.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.viewModelScope

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

    private var isPermissionGranted = false
    private var isTermAgreed = false


    fun onIntent(intent: IntroIntent){
        when(intent){
            is IntroIntent.LoadInitial -> checkInitialState()
            is IntroIntent.NavigateToTerm -> navTo()
            is IntroIntent.NavigateToPermission -> navTo()
            is IntroIntent.NavigateToAdmin -> navigateToAdmin()
            is IntroIntent.NavigateToAerometer -> navigateToAerometer()
        }
    }

    private fun checkInitialState() {
        _uiState.value = IntroUiState.Loading
        viewModelScope.launch {
            delay(1500)
            _uiState.value = IntroUiState.Start
            //추후에 자동 로그인 구현
        }
    }

    private fun navTo() {
        viewModelScope.launch {
            when {
                !isPermissionGranted -> {
                    _uiState.value = IntroUiState.Term
                }

                !isTermAgreed -> {
                    _uiState.value = IntroUiState.Permission
                }

                else -> {
                    _uiState.value = IntroUiState.Start
                }
            }
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
