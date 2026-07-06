package com.reborn.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornSwitch
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun SettingToggleItem(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: (Boolean) = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = RebornTheme.typography.titleMedium,
            color = if(isDark) RebornTheme.color.grayScale100 else RebornTheme.color.grayScale900
        )
        RebornSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


@Composable
fun SettingItem(
    label: String,
    onClick: () -> Unit,
    isDark: Boolean = false
) {
    val textColor = if (isDark) RebornTheme.color.grayScale100 else RebornTheme.color.grayScale900
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = RebornTheme.typography.titleMedium,
        color = textColor
    )
}
