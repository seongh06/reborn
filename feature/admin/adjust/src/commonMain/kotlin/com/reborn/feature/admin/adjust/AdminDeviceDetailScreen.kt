package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.DataType
import com.reborn.core.ui.component.SensorChip
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.component.DeviceSection
import com.reborn.feature.admin.adjust.component.TabBar
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import com.reborn.feature.admin.adjust.model.Device
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed
import com.reborn.feature.admin.adjust.screen.AutoControlScreen
import com.reborn.feature.admin.adjust.screen.RemoteControlScreen

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
    onSendAutoControlClick: (AutoControlUiState) -> Unit = {},
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

    val initialAutoControlState = remember { AutoControlUiState() }
    var autoControlState by remember { mutableStateOf(initialAutoControlState) }

    val isAutoControlChanged = autoControlState != initialAutoControlState

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "IoT 기기 상세보기", onBackClick = onBackClick)
        DeviceSection(
            Device(
                id = state.device.id.toString(),
                name = state.device.name,
                place = state.device.place,
                isOnline = state.device.isOnline,
                isPowerOn = state.device.isPowerOn,
                deviceType = state.device.deviceType
            )
        )
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
                AdminAdjustUiState.ControlMethod.Remote -> RemoteControlScreen(
                    temperature = temperature,
                    onTemperatureChange = { temperature = it },
                    operationMode = operationMode,
                    onOperationModeChange = { operationMode = it },
                    windSpeed = windSpeed,
                    onWindSpeedChange = { windSpeed = it },
                    isPowerOn = isPowerOn,
                    onPowerChange = { isPowerOn = it }
                )
                AdminAdjustUiState.ControlMethod.MANUALEdit -> AutoControlScreen(
                    state = autoControlState,
                    onStateChange = { autoControlState = it }
                )
            }
        }

        if (currentTab == AdminAdjustUiState.ControlMethod.Remote) {
            RebornButton(
                text = "제어 명령 전송",
                enabled = isChanged,
                onClick = { onSendControlClick(temperature, operationMode, windSpeed, isPowerOn) }
            )
        }
        if (currentTab == AdminAdjustUiState.ControlMethod.MANUALEdit) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ){
                RebornButton(
                    modifier = Modifier.weight(1f),
                    text = "기본 값으로 초기화",
                    enabled = isAutoControlChanged,
                    backgroundColor = if (isAutoControlChanged) RebornTheme.color.grayScale100 else RebornTheme.color.grayScale600,
                    onClick = { autoControlState = initialAutoControlState }
                )
                RebornButton(
                    modifier = Modifier.weight(1f),
                    text = "저장",
                    enabled = isAutoControlChanged,
                    backgroundColor = if (isAutoControlChanged) RebornTheme.color.grayScale400 else RebornTheme.color.grayScale600,
                    onClick = { onSendAutoControlClick(autoControlState) }
                )

            }
        }
    }
}