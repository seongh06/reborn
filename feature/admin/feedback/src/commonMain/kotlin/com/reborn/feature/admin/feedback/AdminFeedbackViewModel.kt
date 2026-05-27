package com.reborn.feature.admin.feedback

import androidx.lifecycle.ViewModel
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminFeedbackViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminFeedbackUiState())
    val uiState: StateFlow<AdminFeedbackUiState> = _uiState.asStateFlow()
}
