package com.reborn.feature.admin.adjust.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminAdjustUiState {
    data object Loading : AdminAdjustUiState
    data class Adjust(val devices: List<DeviceItem> = emptyList()) : AdminAdjustUiState
    data object AddDevice : AdminAdjustUiState

    data class DeviceItem(
        val id: Int,
        val place: String,
        val name: String,
        val isOnline: Boolean,
        val isPowerOn: Boolean
    )
}

sealed interface AdminAdjustIntent {
    data object LoadInitial : AdminAdjustIntent
    data object NavigateBack : AdminAdjustIntent
    data object NavigateToAddDevice : AdminAdjustIntent
    data class TogglePower(val deviceId: Int) : AdminAdjustIntent
    data class AddDevice(val place: String, val name: String) : AdminAdjustIntent
}
