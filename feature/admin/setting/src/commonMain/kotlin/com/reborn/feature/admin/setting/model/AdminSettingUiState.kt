package com.reborn.feature.admin.setting.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminSettingUiState {
    data object Loading : AdminSettingUiState
    data class Setting(
        val rooms: List<RoomItem> = emptyList(),
        val profileName: String? = null,
        // 서버에 프로필 이미지 조회 API(#155)는 있으나 로그인 이후 이미지가 없는 유저(카카오 미동의 등)는 null일 수 있음
        val profileImageUrl: String? = null
    ) : AdminSettingUiState

    data class RoomItem(
        val placeId: Int,
        val roomName: String,
        // 상세 조회(#28) 실패 시 null - 실제 0명/0대와 구분해서 표시
        val adminCount: Int?,
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
    data object ClickWithdraw : AdminSettingIntent
    data class UpdateProfileName(val name: String) : AdminSettingIntent
}
