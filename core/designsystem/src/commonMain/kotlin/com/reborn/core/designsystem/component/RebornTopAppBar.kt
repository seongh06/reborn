package com.reborn.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.Res
import com.reborn.core.designsystem.ic_back
import com.reborn.core.designsystem.ic_download
import com.reborn.core.designsystem.ic_notification
import com.reborn.core.designsystem.ic_plus
import com.reborn.core.designsystem.ic_qr
import com.reborn.core.designsystem.ic_setting
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun RebornTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onNavigateAlert: (() -> Unit)? = null,
    onNavigateSetting: (() -> Unit)? = null,
    onNavigateAddDevice: (() -> Unit)? = null,
    onNavigateFeedbackQR: (() -> Unit)? = null,
    onNavigateDataExport: (() -> Unit)? = null,
    darkTheme: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp, 8.dp)
    ) {
        onBackClick?.let {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RebornIcon(
                    color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                    icon = Res.drawable.ic_back,
                    onClick = onBackClick
                )
                title?.let {
                    Text(
                        text = title,
                        style = RebornTheme.typography.titleLarge,
                        color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100
                    )
                }
            }
        }

        if(onBackClick == null){
            title?.let{
                Text(
                    text = title,
                    style = RebornTheme.typography.displayLarge,
                    color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp)
                )
            }
        }

        if (onNavigateAlert != null || onNavigateSetting != null || onNavigateAddDevice != null || onNavigateFeedbackQR != null || onNavigateDataExport != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateAlert != null) {
                    RebornIcon(
                        color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                        icon = Res.drawable.ic_notification,
                        onClick = onNavigateAlert
                    )
                }
                if (onNavigateSetting != null) {
                    RebornIcon(
                        color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                        icon = Res.drawable.ic_setting,
                        onClick = onNavigateSetting
                    )
                }
                if (onNavigateAddDevice != null) {
                    RebornIcon(
                        color = if(!darkTheme)RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                        icon = Res.drawable.ic_plus,
                        onClick = onNavigateAddDevice
                    )
                }
                if (onNavigateFeedbackQR != null) {
                    RebornIcon(
                        color = if (!darkTheme) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                        icon = Res.drawable.ic_qr,
                        onClick = onNavigateFeedbackQR
                    )
                }
                if (onNavigateDataExport != null) {
                    RebornIcon(
                        color = if (!darkTheme) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale100,
                        icon = Res.drawable.ic_download,
                        onClick = onNavigateDataExport
                    )
                }
            }
        }
    }
}