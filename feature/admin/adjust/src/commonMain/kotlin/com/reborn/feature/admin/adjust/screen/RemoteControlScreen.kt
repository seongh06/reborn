package com.reborn.feature.admin.adjust.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SelectPickerSection
import com.reborn.feature.admin.adjust.component.section.RulerPickerSection
import com.reborn.feature.admin.adjust.component.section.SwitchSection
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed

// 온도/운전모드/바람세기는 SmartThings 에어컨 전용 capability(airConditionerMode 등)로만
// 매핑돼서(server SmartThingsDeviceService) 에어컨 외 기기(조명/플러그/TV/커튼/기타)에는
// 의미가 없다 - 전원 on/off(switch capability, 모든 SmartThings 기기 공통)만 노출한다.
@Composable
fun RemoteControlScreen(
    deviceType: DeviceType,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    operationMode: OperationMode,
    onOperationModeChange: (OperationMode) -> Unit,
    windSpeed: WindSpeed,
    onWindSpeedChange: (WindSpeed) -> Unit,
    isPowerOn: Boolean,
    onPowerChange: (Boolean) -> Unit,
) {
    if (deviceType != DeviceType.AIR_CONDITIONER) {
        Column(
            modifier = Modifier.padding(12.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SwitchSection(
                isPower = isPowerOn,
                onPowerChange = onPowerChange
            )
        }
        return
    }

    Column(
        modifier = Modifier.padding(12.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SwitchSection(
            isPower = isPowerOn,
            onPowerChange = onPowerChange
        )
        RulerPickerSection(
            title = "희망 온도",
            value = temperature,
            onValueChange = onTemperatureChange
        )
        SelectPickerSection(
            title = "운전 모드",
            options = OperationMode.entries,
            selectedOption = operationMode,
            onOptionSelected = onOperationModeChange,
            optionToString = { it.label }
        )
        SelectPickerSection(
            title = "바람 세기",
            options = WindSpeed.entries,
            selectedOption = windSpeed,
            onOptionSelected = onWindSpeedChange,
            optionToString = { it.label }
        )
    }
}