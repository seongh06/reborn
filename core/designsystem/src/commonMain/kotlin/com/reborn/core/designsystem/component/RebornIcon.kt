package com.reborn.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


@Composable
fun RebornIcon(
    icon: DrawableResource,
    color: Color = RebornTheme.color.grayScale900,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier
            .size(48.dp)
            .background(Color.Transparent),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "icon",
            modifier = Modifier.size(24.dp),
            tint = color
        )
    }
}