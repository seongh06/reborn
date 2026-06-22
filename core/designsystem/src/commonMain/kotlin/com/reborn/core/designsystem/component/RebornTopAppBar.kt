package com.reborn.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import reborn.core.designsystem.generated.resources.Res
import reborn.core.designsystem.generated.resources.ic_back
import reborn.core.designsystem.generated.resources.ic_notification
import reborn.core.designsystem.generated.resources.ic_setting

@Composable
fun RebornTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onNavigateAlert: (() -> Unit)? = null,
    onNavigateSetting: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(16.dp, 4.dp)
    ) {
        onBackClick?.let {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RebornIcon(
                    icon = Res.drawable.ic_back,
                    onClick = onBackClick
                )

                title?.let {
                    Text(
                        text = title,
                        style = RebornTheme.typography.titleLarge,
                        color = RebornTheme.color.grayScale900
                    )

                }
            }
        }

        if(onBackClick == null){
            title?.let{
                Text(
                    text = title,
                    style = RebornTheme.typography.displayLarge,
                    color = RebornTheme.color.grayScale900,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        if (onNavigateAlert != null && onNavigateSetting != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateAlert != null) {
                    RebornIcon(
                        icon = Res.drawable.ic_notification,
                        onClick = onNavigateAlert
                    )
                }
                if (onNavigateSetting != null) {
                    RebornIcon(
                        icon = Res.drawable.ic_setting,
                        onClick = onNavigateSetting
                    )
                }
            }
        }
    }
}