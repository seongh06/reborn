package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.DataType
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SensorChip
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.component.DeviceSection
import com.reborn.feature.admin.adjust.component.TabBar
import com.reborn.feature.admin.adjust.component.remote.RulerPickerSection
import com.reborn.feature.admin.adjust.component.remote.SelectPickerSection
import com.reborn.feature.admin.adjust.component.remote.SwitchSection
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.Device
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed

@Composable
fun AdminDeviceDetailScreen(
    state: AdminAdjustUiState.DeviceDetail,
    onBackClick: () -> Unit,
    onTabClick: (AdminAdjustUiState.ControlMethod) -> Unit = {},
    onSendControlClick: (
        temperature: Float,
        operationMode: OperationMode,
        windSpeed: WindSpeed,
        isPowerOn: Boolean
    ) -> Unit = { _, _, _, _ -> },
) {

    val currentTab = state.selectedTab

    val initialTemperature = remember { 24f }
    val initialOperationMode = remember { OperationMode.COOL }
    val initialWindSpeed = remember { WindSpeed.AUTO }
    val initialPowerOn = remember { true }

    var temperature by remember { mutableFloatStateOf(initialTemperature) }
    var operationMode by remember { mutableStateOf(initialOperationMode) }
    var windSpeed by remember { mutableStateOf(initialWindSpeed) }
    var isPowerOn by remember { mutableStateOf(initialPowerOn) }

    val isChanged = temperature != initialTemperature ||
        operationMode != initialOperationMode ||
        windSpeed != initialWindSpeed ||
        isPowerOn != initialPowerOn

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "IoT 기기 상세보기", onBackClick = onBackClick)
        DeviceSection(Device("1", "name", "place", true, true, DeviceType.AIR_CONDITIONER))
        Column(
            modifier = Modifier.padding(12.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "현재 센서 상태",
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                SensorChip(type = DataType.Temperature, value = 12)
                SensorChip(type = DataType.Humidity, value = 12)
                SensorChip(type = DataType.Illuminance, value = 12)
                SensorChip(type = DataType.PeopleCount, value = 12)
            }
        }
        TabBar(
            tabItems = AdminAdjustUiState.ControlMethod.entries,
            selectedTab = currentTab,
            onTabSelected = onTabClick,
            getDisplayName = { it.method }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (currentTab) {
                AdminAdjustUiState.ControlMethod.Remote -> RemoteControlSections(
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    operationMode = operationMode,
                    onOperationModeChange = { operationMode = it },
                    windSpeed = windSpeed,
                    onWindSpeedChange = { windSpeed = it },
                    isPowerOn = isPowerOn,
                    onPowerChange = { isPowerOn = it }
                )
                AdminAdjustUiState.ControlMethod.MANUALEdit -> AutoControlContent()
            }
        }

        if (currentTab == AdminAdjustUiState.ControlMethod.Remote) {
            RebornButton(
                text = "제어 명령 전송",
                enabled = isChanged,
                onClick = { onSendControlClick(temperature, operationMode, windSpeed, isPowerOn) }
            )
        }
    }
}

@Composable
private fun RemoteControlSections(
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

@Composable
private fun AutoControlContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "준비 중인 기능입니다",
            style = RebornTheme.typography.bodyMedium,
            color = RebornTheme.color.grayScale500
        )
    }
}