package com.reborn.feature.admin.adjust.component.section

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun <T> SelectPickerSection(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionToString: (T) -> String = { it.toString() }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) RebornTheme.color.grayScale600 else RebornTheme.color.grayScale100,
                    label = "BgColorAnimation"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) RebornTheme.color.grayScale100 else RebornTheme.color.grayScale600,
                    label = "TextColorAnimation"
                )
                val borderColor = if (isSelected) Color.Transparent else RebornTheme.color.grayScale600

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onOptionSelected(option)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionToString(option),
                        style = RebornTheme.typography.labelMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}