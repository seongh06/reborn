package com.reborn.feature.admin.adjust.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.SelectPickerSection
import com.reborn.feature.admin.adjust.component.section.RulerPickerSection
import com.reborn.feature.admin.adjust.component.section.SwitchSection
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed

@Composable
fun RemoteControlScreen(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    operationMode: OperationMode,
    onOperationModeChange: (OperationMode) -> Unit,
    windSpeed: WindSpeed,
    onWindSpeedChange: (WindSpeed) -> Unit,
    isPowerOn: Boolean,
    onPowerChange: (Boolean) -> Unit,
) {
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
            title = "운전모드",
            options = OperationMode.entries,
            selectedOption = operationMode,
            onOptionSelected = onOperationModeChange,
            optionToString = { it.label }
        )
        SelectPickerSection(
            title = "바람세기",
            options = WindSpeed.entries,
            selectedOption = windSpeed,
            onOptionSelected = onWindSpeedChange,
            optionToString = { it.label }
        )
    }
}