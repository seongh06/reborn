package com.reborn.feature.admin.adjust.model

import androidx.compose.runtime.Immutable
import com.reborn.core.model.Metric
import com.reborn.core.ui.component.DeviceType

// SmartThings 기기의 실제 현재 전원/운전모드/바람세기/희망온도(#221) - 필드가 null이면 이 기기가 그
// 컨트롤을 지원하지 않는다는 뜻. deviceStatus 자체가 null이면(DeviceDetail) 아직 조회 전이거나
// SMART_THINGS가 아니라 애초에 조회 대상이 아님(ARDUINO/AI_SPEAKER는 기존과 동일하게 전원만 노출).
data class DeviceCurrentStatus(
    val isPowerOn: Boolean?,
    val operationMode: OperationMode?,
    val windSpeed: WindSpeed?,
    val temperature: Float?,
)

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
        val autoControlState: AutoControlUiState? = null,
        // null이면 아직 조회 전(#221) - 성공하면 채워짐, 실패해도 조용히 null 유지(하드코딩 기본값으로 폴백)
        val metric: Metric? = null,
        val deviceStatus: DeviceCurrentStatus? = null,
        // 최초 접속 튜토리얼(#240) - 원격/자동 제어 탭 각각 처음 볼 때 해당 탭의 컨텐츠 영역을
        // 강조한다. 서로 독립이라 둘 다 true일 수 있고, 화면은 currentTab에 맞는 것만 보여준다.
        val showRemoteTabHint: Boolean = false,
        val showAutoTabHint: Boolean = false,
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
        val deviceType: DeviceType = DeviceType.OTHER,
        // UI 카테고리(deviceType)와 별개로 실제 서버 기기 유형("ARDUINO"/"SMART_THINGS"/"AI_SPEAKER") -
        // 현재 상태 조회(#221)는 SMART_THINGS에만 의미가 있어 이 값으로 호출 여부를 가른다.
        val serverDeviceType: String = "",
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
        // 에어컨 외 기기는 전원 on/off만 보내고 나머지는 null(서버 ControlRequest가 전부 optional)
        val temperature: Float? = null,
        val operationMode: OperationMode? = null,
        val windSpeed: WindSpeed? = null,
        val isPowerOn: Boolean
    ) : AdminAdjustIntent
    data class SendAutoControl(
        val deviceId: String,
        val autoControlState: AutoControlUiState
    ) : AdminAdjustIntent
    data class DeleteDevice(val deviceId: String) : AdminAdjustIntent
    data class DismissTutorial(val stepId: String) : AdminAdjustIntent
}
