package com.reborn.feature.admin.setting.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AdminSettingUiState {
    data object Loading : AdminSettingUiState
    data class Setting(
        val rooms: List<RoomItem> = emptyList(),
        val profileName: String? = null,
        // 서버에 프로필 이미지 조회 API(#155)는 있으나 로그인 이후 이미지가 없는 유저(카카오 미동의 등)는 null일 수 있음
        val profileImageUrl: String? = null,
    ) : AdminSettingUiState

    data class RoomItem(
        val placeId: Int,
        val roomName: String,
        // 방장(#추가 API) 여부 - 방장만 장소를 하드 삭제하거나 방장을 위임할 수 있고,
        // 그 외 관리자는 나가기(leave)만 할 수 있다.
        val isOwner: Boolean = false,
        // 관리자 목록 조회(#217) 실패 시 빈 리스트 - place는 등록 시 최소 1명(등록자)이 항상 ADMIN이라
        // 정상 조회된 결과가 진짜로 비어있는 경우는 없음, 실패와 구분할 필요가 없어 null 대신 emptyList
        val admins: List<AdminProfile> = emptyList()
    )

    data class AdminProfile(
        val userId: Long,
        val name: String,
        val profileImage: String?,
        val isOwner: Boolean = false,
    )
}

sealed interface AdminSettingIntent {
    data object LoadInitial : AdminSettingIntent
    data object NavigateBack : AdminSettingIntent
    data class DeleteRoom(val placeId: Int) : AdminSettingIntent
    data class LeaveRoom(val placeId: Int) : AdminSettingIntent
    data class TransferOwner(val placeId: Int, val newOwnerUserId: Long) : AdminSettingIntent
    data class ClickAddAdmin(val placeId: Int) : AdminSettingIntent
    data class ClickAddDevice(val placeId: Int) : AdminSettingIntent
    data class ClickAddArduino(val placeId: Int) : AdminSettingIntent
    data class ClickAddAiSpeaker(val placeId: Int) : AdminSettingIntent
    data object ClickAddPlace : AdminSettingIntent
    data object ClickLogout : AdminSettingIntent
    data object ClickWithdraw : AdminSettingIntent
    data class UpdateProfileName(val name: String) : AdminSettingIntent
    data class UpdateProfileImage(val bytes: ByteArray, val fileName: String, val mimeType: String) : AdminSettingIntent
}
