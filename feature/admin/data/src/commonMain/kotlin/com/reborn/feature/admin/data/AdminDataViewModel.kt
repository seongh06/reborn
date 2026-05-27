package com.reborn.feature.admin.data

import androidx.lifecycle.ViewModel
import com.reborn.feature.admin.data.model.AdminDataUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminDataViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDataUiState())
    val uiState: StateFlow<AdminDataUiState> = _uiState.asStateFlow()
}
