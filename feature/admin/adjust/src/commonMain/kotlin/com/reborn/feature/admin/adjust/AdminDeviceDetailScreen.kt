package com.reborn.feature.admin.adjust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.model.TutorialStep
import com.reborn.core.ui.component.DataType
import com.reborn.core.ui.component.DeviceType
import com.reborn.core.ui.component.SensorChip
import com.reborn.core.ui.component.TabBar
import com.reborn.core.ui.component.TutorialHighlightOverlay
import com.reborn.core.ui.component.tutorialTarget
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.component.DeviceSection
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import com.reborn.feature.admin.adjust.model.Device
import com.reborn.feature.admin.adjust.model.OperationMode
import com.reborn.feature.admin.adjust.model.WindSpeed
import com.reborn.feature.admin.adjust.model.defaultAutoControlState
import com.reborn.feature.admin.adjust.screen.AutoControlScreen
import com.reborn.feature.admin.adjust.screen.RemoteControlScreen

// 최초 접속 튜토리얼(#240) 설명 문구 - 바텀 네비 자리에 대신 뜨는 TutorialHintCard(App.kt)에서 쓴다.
private const val REMOTE_TAB_TUTORIAL_HINT = "여기서 전원, 온도, 바람세기 등을 바로 조절할 수 있어요."
private const val AUTO_TAB_TUTORIAL_HINT = "조건을 설정해두면 알아서 자동으로 제어돼요. 규칙을 만들어보세요!"

@Composable
fun AdminDeviceDetailScreen(
    state: AdminAdjustUiState.DeviceDetail,
    onBackClick: () -> Unit,
    onTabClick: (AdminAdjustUiState.ControlMethod) -> Unit = {},
    onSendControlClick: (
        temperature: Float?,
        operationMode: OperationMode?,
        windSpeed: WindSpeed?,
        isPowerOn: Boolean
    ) -> Unit = { _, _, _, _ -> },
    onSendAutoControlClick: (AutoControlUiState) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onDismissTutorial: (String) -> Unit = {},
) {
    val deviceType = state.device.deviceType
    val isSmartThings = state.device.serverDeviceType == "SMART_THINGS"
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var tabContentRect by remember { mutableStateOf<Rect?>(null) }

    val currentTab = state.selectedTab

    // SmartThings 기기는 실제 조회 결과(#221)를, 조회 전(deviceStatus == null)이거나 SmartThings가
    // 아닌 기기는 기존 프리셋 기본값을 초기값으로 쓴다 - deviceStatus가 로드되면 그 시점 값으로 편집
    // 기준선(initial*)도 함께 다시 잡혀야 하므로 remember 키에 deviceStatus를 포함한다.
    val status = state.deviceStatus
    val initialTemperature = remember(state.deviceId, status) { status?.temperature ?: 24f }
    val initialOperationMode = remember(state.deviceId, status) { status?.operationMode ?: OperationMode.COOL }
    val initialWindSpeed = remember(state.deviceId, status) { status?.windSpeed ?: WindSpeed.AUTO }
    val initialPowerOn = remember(state.deviceId, status) { status?.isPowerOn ?: state.device.isPowerOn }

    var temperature by remember(state.deviceId, status) { mutableFloatStateOf(initialTemperature) }
    var operationMode by remember(state.deviceId, status) { mutableStateOf(initialOperationMode) }
    var windSpeed by remember(state.deviceId, status) { mutableStateOf(initialWindSpeed) }
    var isPowerOn by remember(state.deviceId, status) { mutableStateOf(initialPowerOn) }

    // 이 기기가 실제로 지원하는 컨트롤만 보여준다(#221) - SmartThings 조회 결과가 아직 없으면(로딩 중)
    // 우선 다 보여주고, 로드 완료 후 null인 필드는 화면에서 숨긴다. SmartThings가 아니면 전원만 노출.
    val supportsOperationMode = isSmartThings && (status == null || status.operationMode != null)
    val supportsWindSpeed = isSmartThings && (status == null || status.windSpeed != null)
    val supportsTemperature = isSmartThings && (status == null || status.temperature != null)

    val isChanged = if (isSmartThings) {
        (supportsTemperature && temperature != initialTemperature) ||
            (supportsOperationMode && operationMode != initialOperationMode) ||
            (supportsWindSpeed && windSpeed != initialWindSpeed) ||
            isPowerOn != initialPowerOn
    } else {
        isPowerOn != initialPowerOn
    }

    // 서버에서 규칙을 불러오기 전(null)에는 화면 프리셋 기본값을 보여주다가, 로드/저장 완료 시
    // state.autoControlState가 갱신되면 편집 기준선도 함께 새로 잡는다(#190).
    val initialAutoControlState = remember(state.autoControlState) {
        state.autoControlState ?: defaultAutoControlState(deviceType)
    }
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

    Box {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            RebornTopAppBar(title = "IoT 기기 상세", onBackClick = onBackClick)
            // 기기 해제는 뒤로가기와 같은 상단 바 줄, 우측 끝에 배치한다. 이 화면은
            // RebornTopAppBar의 onNavigate* 트레일링 아이콘을 하나도 쓰지 않으므로(CodeRabbit
            // 리뷰 지적한 겹침 우려는 현재 이 화면 한정으로는 해당 없음) 겹칠 대상이 없다.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp)
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "기기 해제",
                    style = RebornTheme.typography.labelLarge,
                    color = RebornTheme.color.reject,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
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
                // 값이 없는 항목(이 기기가 그 센서를 지원하지 않거나 아직 로딩 전)은 칩 자체를 숨긴다.
                state.metric?.temperature?.let { SensorChip(type = DataType.Temperature, value = it.toFloat()) }
                state.metric?.humidity?.let { SensorChip(type = DataType.Humidity, value = it.toFloat()) }
                state.metric?.illuminance?.let { SensorChip(type = DataType.Illuminance, value = it.toFloat()) }
                state.metric?.peopleCount?.let { SensorChip(type = DataType.PeopleCount, value = it.toFloat()) }
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
                .tutorialTarget { tabContentRect = it }
        ) {
            when (currentTab) {
                AdminAdjustUiState.ControlMethod.Remote -> RemoteControlScreen(
                    supportsTemperature = supportsTemperature,
                    supportsOperationMode = supportsOperationMode,
                    supportsWindSpeed = supportsWindSpeed,
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
                    deviceType = deviceType,
                    state = autoControlState,
                    onStateChange = { autoControlState = it }
                )
            }
        }

        if (currentTab == AdminAdjustUiState.ControlMethod.Remote) {
            RebornButton(
                text = "제어 명령 전송",
                enabled = isChanged,
                onClick = {
                    onSendControlClick(
                        if (supportsTemperature) temperature else null,
                        if (supportsOperationMode) operationMode else null,
                        if (supportsWindSpeed) windSpeed else null,
                        isPowerOn
                    )
                }
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

        // 최초 접속 튜토리얼(#240) - 원격/자동제어 탭 컨텐츠를 각각 처음 볼 때 강조.
        val showTabHint = when (currentTab) {
            AdminAdjustUiState.ControlMethod.Remote -> state.showRemoteTabHint
            AdminAdjustUiState.ControlMethod.MANUALEdit -> state.showAutoTabHint
        }
        if (showTabHint) {
            TutorialHighlightOverlay(
                highlightRect = tabContentRect,
                onDismiss = {
                    val stepId = when (currentTab) {
                        AdminAdjustUiState.ControlMethod.Remote -> TutorialStep.ADJUST_REMOTE_TAB
                        AdminAdjustUiState.ControlMethod.MANUALEdit -> TutorialStep.ADJUST_AUTO_TAB
                    }
                    onDismissTutorial(stepId)
                }
            )
        }
    }
}