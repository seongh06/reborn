package com.reborn.feature.intro

import androidx.lifecycle.ViewModel
import com.reborn.feature.intro.model.IntroUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()
}
