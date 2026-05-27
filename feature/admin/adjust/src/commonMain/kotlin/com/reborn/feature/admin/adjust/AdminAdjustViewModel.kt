package com.reborn.feature.admin.adjust

import androidx.lifecycle.ViewModel
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminAdjustViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminAdjustUiState())
    val uiState: StateFlow<AdminAdjustUiState> = _uiState.asStateFlow()
}
