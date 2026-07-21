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
        // 상세 조회(#28) 실패 시 null - 실제 0대와 구분해서 표시
        val deviceCount: Int?
    )
}

sealed interface AdminSettingIntent {
    data object LoadInitial : AdminSettingIntent
    data object NavigateBack : AdminSettingIntent
    data class DeleteRoom(val placeId: Int) : AdminSettingIntent
    data class ClickAddAdmin(val placeId: Int) : AdminSettingIntent
    data class ClickAddDevice(val placeId: Int) : AdminSettingIntent
    data class ClickAddArduino(val placeId: Int) : AdminSettingIntent
    data class ClickAddAiSpeaker(val placeId: Int) : AdminSettingIntent
    data object ClickAddPlace : AdminSettingIntent
    data object ClickLogout : AdminSettingIntent
}
