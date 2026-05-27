package com.reborn.feature.aerometer

import androidx.lifecycle.ViewModel
import com.reborn.feature.aerometer.model.AerometerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AerometerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AerometerUiState())
    val uiState: StateFlow<AerometerUiState> = _uiState.asStateFlow()
}
