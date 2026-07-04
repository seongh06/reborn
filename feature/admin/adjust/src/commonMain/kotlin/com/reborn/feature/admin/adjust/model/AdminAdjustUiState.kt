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
        val deviceId: Int
    ) : AdminAdjustUiState

    enum class ControlMethod(val method: String) {
        Remote("원격 제어"),
        MANUALEdit("자동 제어")
    }


    data class DeviceItem(
        val id: Int,
        val place: String,
        val name: String,
        val isOnline: Boolean,
        val isPowerOn: Boolean,
        val deviceType: DeviceType = DeviceType.OTHER
    )
}

sealed interface AdminAdjustIntent {
    data object LoadInitial : AdminAdjustIntent
    data object NavigateBack : AdminAdjustIntent
    data object NavigateToAddDevice : AdminAdjustIntent
    data class NavigateToDeviceDetail(
        val controlMethod: AdminAdjustUiState.ControlMethod = AdminAdjustUiState.ControlMethod.Remote,
        val deviceId : Int
    ) : AdminAdjustIntent
    data class TogglePower(val deviceId: Int) : AdminAdjustIntent
    data class AddDevice(val place: String, val name: String) : AdminAdjustIntent
    data class ClickTab(val tab: AdminAdjustUiState.ControlMethod) : AdminAdjustIntent
    data class SendRemoteControl(
        val deviceId: Int,
        val temperature: Float,
        val operationMode: OperationMode,
        val windSpeed: WindSpeed,
        val isPowerOn: Boolean
    ) : AdminAdjustIntent
}
