package com.reborn.feature.admin.adjust.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.DeviceType
import com.reborn.feature.admin.adjust.component.section.ActionSelectionBottomSheet
import com.reborn.feature.admin.adjust.component.section.AutomationRuleSection
import com.reborn.feature.admin.adjust.model.AutoControlField
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import com.reborn.feature.admin.adjust.model.RuleData
import com.reborn.feature.admin.adjust.model.SimpleAutoControlActions
import com.reborn.feature.admin.adjust.model.SimpleAutoControlField
import com.reborn.feature.admin.adjust.model.ToggleOptionData
import com.reborn.feature.admin.adjust.model.applyAction
import com.reborn.feature.admin.adjust.model.applySimpleAction

private val NonNumericThresholdChars = Regex("[^0-9.%°C명]")
private val NonDigitChars = Regex("[^0-9]")

private fun sanitizeThreshold(value: String): String = value.replace(NonNumericThresholdChars, "")
private fun sanitizeMinutes(value: String): String = value.replace(NonDigitChars, "")

// 서버 자동 제어 실행(AutoControlEvaluationService)이 온도 상/하한 조건만 실제로 실행하고,
// 액션도 에어컨 전용 운전모드 문구로만 해석돼서 - 에어컨 외 기기(조명/플러그/TV/커튼/기타)는
// 실내 온도 규칙 하나만, 액션은 켜기/끄기로 축소된 화면을 보여준다.
@Composable
fun AutoControlScreen(
    deviceType: DeviceType,
    state: AutoControlUiState,
    onStateChange: (AutoControlUiState) -> Unit
) {
    if (deviceType != DeviceType.AIR_CONDITIONER) {
        SimpleAutoControlScreen(state = state, onStateChange = onStateChange)
        return
    }

    var editingField by remember { mutableStateOf<AutoControlField?>(null) }

    Column(
        modifier = Modifier.padding(12.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AutomationRuleSection(
            title = "불쾌지수 규칙",
            ruleRows = listOf(
                RuleData(
                    id = "discomfort",
                    inputValue = state.discomfortThreshold,
                    onInputValueChange = { onStateChange(state.copy(discomfortThreshold = it)) },
                    conditionLabel = "이상 ➔",
                    actionText = state.discomfortAction,
                    onActionClick = { editingField = AutoControlField.Discomfort },
                    keyboardType = KeyboardType.Text
                )
            )
        )
        AutomationRuleSection(
            title = "실내 습도 규칙",
            ruleRows = listOf(
                RuleData(
                    id = "humidityHigh",
                    inputValue = state.humidityHighThreshold,
                    onInputValueChange = { onStateChange(state.copy(humidityHighThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이상 ➔",
                    actionText = state.humidityHighAction,
                    onActionClick = { editingField = AutoControlField.HumidityHigh },
                    keyboardType = KeyboardType.Number
                ),
                RuleData(
                    id = "humidityLow",
                    inputValue = state.humidityLowThreshold,
                    onInputValueChange = { onStateChange(state.copy(humidityLowThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이하 ➔",
                    actionText = state.humidityLowAction,
                    onActionClick = { editingField = AutoControlField.HumidityLow },
                    keyboardType = KeyboardType.Number
                )
            )
        )
        AutomationRuleSection(
            title = "실내 온도 규칙",
            ruleRows = listOf(
                RuleData(
                    id = "temperatureHigh",
                    inputValue = state.temperatureHighThreshold,
                    onInputValueChange = { onStateChange(state.copy(temperatureHighThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이상 ➔",
                    actionText = state.temperatureHighAction,
                    onActionClick = { editingField = AutoControlField.TemperatureHigh },
                    keyboardType = KeyboardType.Number
                ),
                RuleData(
                    id = "temperatureLow",
                    inputValue = state.temperatureLowThreshold,
                    onInputValueChange = { onStateChange(state.copy(temperatureLowThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이하 ➔",
                    actionText = state.temperatureLowAction,
                    onActionClick = { editingField = AutoControlField.TemperatureLow },
                    keyboardType = KeyboardType.Number
                )
            )
        )
        AutomationRuleSection(
            title = "재실 인원 밀집 규칙",
            ruleRows = listOf(
                RuleData(
                    id = "occupancy",
                    inputValue = state.occupancyThreshold,
                    onInputValueChange = { onStateChange(state.copy(occupancyThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이상 ➔",
                    actionText = state.occupancyAction,
                    onActionClick = { editingField = AutoControlField.Occupancy },
                    keyboardType = KeyboardType.Number
                )
            ),
            toggleOption = ToggleOptionData(
                isChecked = state.isAutoOffEnabled,
                onCheckedChange = { onStateChange(state.copy(isAutoOffEnabled = it)) },
                minutesValue = state.autoOffMinutes,
                onMinutesValueChange = { onStateChange(state.copy(autoOffMinutes = sanitizeMinutes(it))) }
            )
        )
    }

    val field = editingField
    if (field != null) {
        ActionSelectionBottomSheet(
            onDismissRequest = { editingField = null },
            actionOptions = field.actionOptions,
            onOptionSelected = { option -> onStateChange(state.applyAction(field, option)) }
        )
    }
}

@Composable
private fun SimpleAutoControlScreen(
    state: AutoControlUiState,
    onStateChange: (AutoControlUiState) -> Unit
) {
    var editingField by remember { mutableStateOf<SimpleAutoControlField?>(null) }

    Column(
        modifier = Modifier.padding(12.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AutomationRuleSection(
            title = "실내 온도 규칙",
            ruleRows = listOf(
                RuleData(
                    id = "temperatureHigh",
                    inputValue = state.temperatureHighThreshold,
                    onInputValueChange = { onStateChange(state.copy(temperatureHighThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이상 ➔",
                    actionText = state.temperatureHighAction,
                    onActionClick = { editingField = SimpleAutoControlField.TemperatureHigh },
                    keyboardType = KeyboardType.Number
                ),
                RuleData(
                    id = "temperatureLow",
                    inputValue = state.temperatureLowThreshold,
                    onInputValueChange = { onStateChange(state.copy(temperatureLowThreshold = sanitizeThreshold(it))) },
                    conditionLabel = "이하 ➔",
                    actionText = state.temperatureLowAction,
                    onActionClick = { editingField = SimpleAutoControlField.TemperatureLow },
                    keyboardType = KeyboardType.Number
                )
            )
        )
    }

    val field = editingField
    if (field != null) {
        ActionSelectionBottomSheet(
            onDismissRequest = { editingField = null },
            actionOptions = SimpleAutoControlActions,
            onOptionSelected = { option -> onStateChange(state.applySimpleAction(field, option)) }
        )
    }
}
