package com.reborn.feature.aerometer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault

@Composable
fun AerometerSettingScreen(
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
            SettingItem(label = "서비스 소개", onClick = {})
            HorizontalDivider(color = RebornTheme.color.grayScale800)
            SettingItem(label = "이용약관", onClick = {})
            HorizontalDivider(color = RebornTheme.color.grayScale800)
            SettingItem(label = "탈퇴", onClick = {})
        }
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
