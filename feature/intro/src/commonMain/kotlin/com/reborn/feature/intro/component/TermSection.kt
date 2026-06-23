package com.reborn.feature.intro.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun TermSection(
    title: String,
    content: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp,8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        isExpanded = !isExpanded
                    },                text = title,
                style = RebornTheme.typography.titleLarge,
                color = RebornTheme.color.grayScale700
            )
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedBoxColor = RebornTheme.color.grayScale500,
                    checkedBorderColor = RebornTheme.color.grayScale500,
                    checkedCheckmarkColor = RebornTheme.color.grayScale100,
                    uncheckedBoxColor = Color.Transparent,
                    uncheckedBorderColor = RebornTheme.color.grayScale500,
                    uncheckedCheckmarkColor = Color.Transparent
                )
            )
        }
        if (isExpanded) {
            Text(
                text = content,
                style = RebornTheme.typography.bodyMedium,
                color = RebornTheme.color.grayScale900
            )
        }
    }
}