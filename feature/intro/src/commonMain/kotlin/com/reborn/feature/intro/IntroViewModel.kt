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

sealed class IntroEvent {
    data object NavigateToAdmin : IntroEvent()
    data object NavigateToAerometer : IntroEvent()
    data object PermissionGranted : IntroEvent()
    data object ExitIntro : IntroEvent()
    data class ShowErrorSnackbar(val throwable: Throwable) : IntroEvent()
}

class IntroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<IntroUiState>(IntroUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<IntroEvent>()
    val event = _event.asSharedFlow()

    private val backStack = mutableListOf<IntroUiState>()

    fun onIntent(intent: IntroIntent){
        when(intent){
            is IntroIntent.LoadInitial -> checkInitialState()
            is IntroIntent.NavigateToTerm -> navigateTo(IntroUiState.Term)
            is IntroIntent.NavigateToPermission -> navigateTo(IntroUiState.Permission)
            is IntroIntent.NavigateToModeSelect -> navigateTo(IntroUiState.ModeSelect)
            is IntroIntent.NavigateToAdminLogin -> navigateTo(IntroUiState.AdminLogin)
            is IntroIntent.NavigateToAdminModeSelect -> navigateTo(IntroUiState.AdminModeSelect)
            is IntroIntent.NavigateToAdminPlaceName -> navigateTo(IntroUiState.AdminPlaceName)
            is IntroIntent.NavigateToAdminPlaceSelect -> navigateTo(IntroUiState.AdminPlaceSelect)
            is IntroIntent.NavigateToAerometerPairing -> navigateTo(IntroUiState.AerometerPairing)
            is IntroIntent.NavigateToInviteCode -> navigateTo(IntroUiState.InviteCode)
            is IntroIntent.NavigateToAdminCode -> navigateTo(IntroUiState.AdminCode)
            is IntroIntent.NavigateBack -> navigateBack()
            is IntroIntent.PermissionsGranted -> onPermissionsGranted()
            is IntroIntent.NavigateToAdmin -> navigateToAdmin()
            is IntroIntent.NavigateToAerometer -> navigateToAerometer()
        }
    }

    private fun navigateTo(next: IntroUiState) {
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
                _event.emit(IntroEvent.ExitIntro)
            }
        }
    }

    private fun checkInitialState() {
        backStack.clear()
        _uiState.value = IntroUiState.Loading
        viewModelScope.launch {
            delay(1500)
            _uiState.value = IntroUiState.Start
        }
    }

    private fun onPermissionsGranted() {
        viewModelScope.launch {
            _event.emit(IntroEvent.PermissionGranted)
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
