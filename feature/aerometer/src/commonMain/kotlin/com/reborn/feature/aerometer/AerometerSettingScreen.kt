package com.reborn.feature.aerometer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault

@Composable
fun AerometerSettingScreen(
    isSaveImageEnabled: Boolean,
    onToggleSaveImage: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale900)
    ) {
        RebornTopAppBar(onBackClick = onBackClick, title = "설정", darkTheme = true)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = RebornTheme.color.grayScale800,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            SettingToggleItem(
                label = "이미지 저장",
                checked = isSaveImageEnabled,
                onCheckedChange = { onToggleSaveImage() }
            )
            HorizontalDivider(color = RebornTheme.color.grayScale800)
            SettingItem(label = "서비스 소개", onClick = {})
            HorizontalDivider(color = RebornTheme.color.grayScale800)
            SettingItem(label = "이용약관", onClick = {})
            HorizontalDivider(color = RebornTheme.color.grayScale800)
            SettingItem(label = "탈퇴", onClick = {})
        }
    }
}

@Composable
private fun SettingToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale100
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RebornTheme.color.grayScale900,
                checkedTrackColor = RebornTheme.color.grayScale100,
                uncheckedThumbColor = RebornTheme.color.grayScale600,
                uncheckedTrackColor = RebornTheme.color.grayScale800,
                uncheckedBorderColor = RebornTheme.color.grayScale700,
            )
        )
    }
}

@Composable
private fun SettingItem(
    label: String,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = RebornTheme.typography.titleMedium,
        color = RebornTheme.color.grayScale100
    )
}
