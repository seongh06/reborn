package com.reborn.feature.admin.adjust.model

import androidx.compose.runtime.Immutable
import com.reborn.core.ui.component.DeviceType

@Immutable
sealed interface AdminAdjustUiState {
    data object Loading : AdminAdjustUiState
    data class Adjust(val devices: List<DeviceItem> = emptyList()) : AdminAdjustUiState
    data object AddDevice : AdminAdjustUiState
    data class DeviceDetail(
        val selectedTab: ControlMethod = ControlMethod.Remote,
        val deviceId: String,
        val device: DeviceItem,
        // null이면 아직 서버에서 불러오지 않은 상태(#190) - MANUALEdit 탭 진입 시 채워짐
        val autoControlState: AutoControlUiState? = null
    ) : AdminAdjustUiState

    enum class ControlMethod(val method: String) {
        Remote("원격 제어"),
        MANUALEdit("자동 제어")
    }


    data class DeviceItem(
        // 서버 deviceKey(SmartThings 기기 ID/Arduino 시리얼) 그 자체 - 목록/네비게이션/제어 API
        // 식별자가 전부 동일한 값이라 별도 로컬 id를 두지 않는다.
        val id: String,
        // 방(room) 개념이 서버 도메인에 없어(#166) deviceType 라벨(아두이노/SmartThings/AI 스피커)로 대체
        val place: String,
        val name: String,
        val isOnline: Boolean,
        // 목록 조회 API가 파워 상태를 내려주지 않아(제어 API로만 변경 가능) 항상 false로 시작 -
        // 실제 초기 상태는 기기별 실시간 조회 API가 없어 알 수 없음(#181에 남은 gap)
        val isPowerOn: Boolean = false,
        val deviceType: DeviceType = DeviceType.OTHER
    )
}

sealed interface AdminAdjustIntent {
    data class LoadInitial(val deviceId: String? = null) : AdminAdjustIntent
    data object NavigateBack : AdminAdjustIntent
    data object NavigateToAddDevice : AdminAdjustIntent
    data class NavigateToDeviceDetail(
        val controlMethod: AdminAdjustUiState.ControlMethod = AdminAdjustUiState.ControlMethod.Remote,
        val deviceId : String
    ) : AdminAdjustIntent
    data class TogglePower(val deviceId: String) : AdminAdjustIntent
    data class AddDevice(val place: String, val name: String) : AdminAdjustIntent
    data class ClickTab(val tab: AdminAdjustUiState.ControlMethod) : AdminAdjustIntent
    data class SendRemoteControl(
        val deviceId: String,
        val temperature: Float,
        val operationMode: OperationMode,
        val windSpeed: WindSpeed,
        val isPowerOn: Boolean
    ) : AdminAdjustIntent
    data class SendAutoControl(
        val deviceId: String,
        val autoControlState: AutoControlUiState
    ) : AdminAdjustIntent
    data class DeleteDevice(val deviceId: String) : AdminAdjustIntent
}
