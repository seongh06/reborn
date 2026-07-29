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

// 전원은 SmartThings 기기 공통(switch capability)이라 항상 노출. 온도/운전모드/바람세기는 기기별로
// 실제 지원 여부가 갈려서(#221, 예: 플러그는 온도 조절 불가) 각 항목을 개별 플래그로 켜고 끈다 -
// 기존엔 "에어컨 카테고리인지"로 전체 블록을 한 번에 켜고 껐지만, 그건 관리자가 등록 시 직접 고른
// 카테고리 라벨일 뿐 실제 기기 capability와 다를 수 있었다.
@Composable
fun RemoteControlScreen(
    supportsTemperature: Boolean,
    supportsOperationMode: Boolean,
    supportsWindSpeed: Boolean,
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
        if (supportsTemperature) {
            RulerPickerSection(
                title = "희망 온도",
                value = temperature,
                onValueChange = onTemperatureChange
            )
        }
        if (supportsOperationMode) {
            SelectPickerSection(
                title = "운전 모드",
                options = OperationMode.entries,
                selectedOption = operationMode,
                onOptionSelected = onOperationModeChange,
                optionToString = { it.label }
            )
        }
        if (supportsWindSpeed) {
            SelectPickerSection(
                title = "바람 세기",
                options = WindSpeed.entries,
                selectedOption = windSpeed,
                onOptionSelected = onWindSpeedChange,
                optionToString = { it.label }
            )
        }
    }
}
