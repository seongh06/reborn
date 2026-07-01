package com.reborn.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NavigationManager<S, E>(
    initialState: S,
    private val exitEvent: E,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<S>(initialState)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<E>()
    val event = _event.asSharedFlow()

    private val backStack = mutableListOf<S>()

    fun navigateTo(next: S) {
        if (_uiState.value == next) return
        backStack.add(_uiState.value)
        _uiState.value = next
    }

    fun navigateBack() {
        val previous = backStack.removeLastOrNull()
        if (previous != null) {
            _uiState.value = previous
        } else {
            scope.launch {
                _event.emit(exitEvent)
            }
        }
    }

    fun clearAndReset(state: S) {
        backStack.clear()
        _uiState.value = state
    }

    fun emitEvent(event: E) {
        scope.launch {
            _event.emit(event)
        }
    }
}