package com.reborn.feature.admin.adjust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.DataType
import com.reborn.core.ui.component.SensorChip
import com.reborn.core.ui.component.TabBar
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.component.DeviceSection
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
    onDeleteClick: () -> Unit = {},
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    // 서버에서 규칙을 불러오기 전(null)에는 화면 프리셋 기본값을 보여주다가, 로드/저장 완료 시
    // state.autoControlState가 갱신되면 편집 기준선도 함께 새로 잡는다(#190).
    val initialAutoControlState = remember(state.autoControlState) { state.autoControlState ?: AutoControlUiState() }
    var autoControlState by remember(state.autoControlState) { mutableStateOf(initialAutoControlState) }

    val isAutoControlChanged = autoControlState != initialAutoControlState

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "기기를 해제할까요?",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
            },
            text = {
                Text(
                    "해제하면 이 장소에서 기기가 제거돼요. 다시 등록하려면 처음부터 다시 연동해야 해요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleteClick() }) {
                    Text("해제", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.reject)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale700)
                }
            }
        )
    }

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "IoT 기기 상세", onBackClick = onBackClick)
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
        Text(
            "기기 해제",
            style = RebornTheme.typography.labelLarge,
            color = RebornTheme.color.reject,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(12.dp, 4.dp)
                .clickable { showDeleteConfirm = true }
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