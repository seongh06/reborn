package com.reborn.feature.admin.setting.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminSettingUiState {
    data object Loading : AdminSettingUiState
    data class Setting(
        val rooms: List<RoomItem> = emptyList()
    ) : AdminSettingUiState

    data class RoomItem(
        val placeId: Int,
        val roomName: String,
        val adminCount: Int,
        val deviceCount: Int
    )
}

sealed interface AdminSettingIntent {
    data object LoadInitial : AdminSettingIntent
    data object NavigateBack : AdminSettingIntent
    data class DeleteRoom(val placeId: Int) : AdminSettingIntent
    data class ClickAddAdmin(val placeId: Int) : AdminSettingIntent
    data class ClickAddDevice(val placeId: Int) : AdminSettingIntent
}
