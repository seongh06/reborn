package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun SensorChip(
    modifier: Modifier = Modifier,
    type: DataType,
    value: Int,
) {
    val style = getUiStyleForType(type)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(RebornTheme.color.grayScale200)
            .padding(12.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ){
        Icon(
            painterResource(style.icon),
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = style.color
        )
        Text(
            value.toString(),
            style = RebornTheme.typography.labelMedium,
            color = RebornTheme.color.grayScale500
        )
    }
}
