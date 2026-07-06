package com.reborn.feature.admin.adjust.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.SettingToggleItem

@Composable
fun SwitchSection(
    isPower: Boolean,
    onPowerChange: (Boolean) -> Unit
){
    SettingToggleItem(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100),
        label = "전원",
        checked = isPower,
        onCheckedChange = onPowerChange
    )
}