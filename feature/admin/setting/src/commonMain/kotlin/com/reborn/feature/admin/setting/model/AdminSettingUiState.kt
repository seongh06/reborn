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
        // 최초 접속 튜토리얼(#240) - 첫 번째 place 카드의 점3개 메뉴를 하이라이트한다. 여러 장소가
        // 있어도 하나만 예시로 보여주면 충분해서 첫 번째 room에만 적용.
        val showPlaceMenuHint: Boolean = false,
        // 최초 접속 튜토리얼(#240) - 점3개 바텀시트를 열었을 때 "아두이노 추가"/"AI 스피커 추가"
        // 항목을 하이라이트한다. 바텀시트는 별도 Popup 레이어라 시트 내부에서 자체적으로 그린다.
        val showAddDeviceHint: Boolean = false,
    ) : AdminSettingUiState

    data class RoomItem(
        val placeId: Int,
        val roomName: String,
        // 관리자 목록 조회(#217) 실패 시 빈 리스트 - place는 등록 시 최소 1명(등록자)이 항상 ADMIN이라
        // 정상 조회된 결과가 진짜로 비어있는 경우는 없음, 실패와 구분할 필요가 없어 null 대신 emptyList
        val admins: List<AdminProfile> = emptyList()
    )

    data class AdminProfile(
        val userId: Long,
        val name: String,
        val profileImage: String?
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
    data class UpdateProfileImage(val bytes: ByteArray, val fileName: String, val mimeType: String) : AdminSettingIntent
    data class DismissTutorial(val stepId: String) : AdminSettingIntent
}
