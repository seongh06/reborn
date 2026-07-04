package com.reborn.feature.admin.adjust.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reborn.core.designsystem.component.RebornSwitch
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.admin.adjust.model.RuleData
import com.reborn.feature.admin.adjust.model.ToggleOptionData

@Composable
fun AutomationRuleSection(
    title: String,
    ruleRows: List<RuleData>,
    modifier: Modifier = Modifier,
    toggleOption: ToggleOptionData? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale900
        )

        ruleRows.forEach { rowData ->
            RuleConditionRow(data = rowData)
        }

        if (toggleOption != null) {
            HorizontalDivider(color = RebornTheme.color.grayScale300, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "0명 시",
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale900
                    )

                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RebornTheme.color.grayScale200)
                            .border(
                                width = 0.5.dp,
                                color = RebornTheme.color.grayScale300,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp, 2.dp)
                    ) {
                        BasicTextField(
                            value = toggleOption.minutesValue,
                            onValueChange = toggleOption.onMinutesValueChange,
                            textStyle = RebornTheme.typography.bodyMedium,
                            cursorBrush = SolidColor(RebornTheme.color.grayScale900),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Text(text = "분 유지 시 전원 자동 off", fontSize = 18.sp, color = Color.Black)
                }

                RebornSwitch(
                    checked = toggleOption.isChecked,
                    onCheckedChange = toggleOption.onCheckedChange,
                )
            }
        }
    }
}

@Composable
fun RuleConditionRow(data: RuleData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RebornTheme.color.grayScale200)
                    .border(
                        width = 0.5.dp,
                        color = RebornTheme.color.grayScale300,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp, 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = data.inputValue,
                    onValueChange = data.onInputValueChange,
                    textStyle = RebornTheme.typography.labelMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = data.keyboardType),
                    singleLine = true
                )
            }
            Text(
                text = data.conditionLabel,
                style = RebornTheme.typography.labelMedium,
                color = RebornTheme.color.grayScale900
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(RebornTheme.color.grayScale200)
                .clickable { data.onActionClick() }
                .border(
                    width = 0.5.dp,
                    color = RebornTheme.color.grayScale300,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp, 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = data.actionText,
                style = RebornTheme.typography.labelMedium,
                color = RebornTheme.color.grayScale900
            )
        }
    }
}