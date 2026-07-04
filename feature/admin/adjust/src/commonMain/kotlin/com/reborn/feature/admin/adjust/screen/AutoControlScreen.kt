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
import com.reborn.feature.admin.adjust.component.section.ActionSelectionBottomSheet
import com.reborn.feature.admin.adjust.component.section.AutomationRuleSection
import com.reborn.feature.admin.adjust.model.AutoControlField
import com.reborn.feature.admin.adjust.model.AutoControlUiState
import com.reborn.feature.admin.adjust.model.RuleData
import com.reborn.feature.admin.adjust.model.ToggleOptionData
import com.reborn.feature.admin.adjust.model.applyAction

@Composable
fun AutoControlScreen(
    state: AutoControlUiState,
    onStateChange: (AutoControlUiState) -> Unit
) {
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
                    onInputValueChange = { onStateChange(state.copy(humidityHighThreshold = it)) },
                    conditionLabel = "이상 ➔",
                    actionText = state.humidityHighAction,
                    onActionClick = { editingField = AutoControlField.HumidityHigh },
                    keyboardType = KeyboardType.Number
                ),
                RuleData(
                    id = "humidityLow",
                    inputValue = state.humidityLowThreshold,
                    onInputValueChange = { onStateChange(state.copy(humidityLowThreshold = it)) },
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
                    onInputValueChange = { onStateChange(state.copy(temperatureHighThreshold = it)) },
                    conditionLabel = "이상 ➔",
                    actionText = state.temperatureHighAction,
                    onActionClick = { editingField = AutoControlField.TemperatureHigh },
                    keyboardType = KeyboardType.Number
                ),
                RuleData(
                    id = "temperatureLow",
                    inputValue = state.temperatureLowThreshold,
                    onInputValueChange = { onStateChange(state.copy(temperatureLowThreshold = it)) },
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
                    onInputValueChange = { onStateChange(state.copy(occupancyThreshold = it)) },
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
                onMinutesValueChange = { onStateChange(state.copy(autoOffMinutes = it)) }
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
