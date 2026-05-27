package com.reborn.feature.admin.setting

import androidx.lifecycle.ViewModel
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminSettingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminSettingUiState())
    val uiState: StateFlow<AdminSettingUiState> = _uiState.asStateFlow()
}
