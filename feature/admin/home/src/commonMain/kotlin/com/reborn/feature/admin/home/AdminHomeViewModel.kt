package com.reborn.feature.admin.home

import androidx.lifecycle.ViewModel
import com.reborn.feature.admin.home.model.AdminHomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminHomeUiState())
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()
}
