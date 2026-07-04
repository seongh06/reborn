package com.reborn.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun RebornSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp = 40.dp,
    height: Dp = 24.dp,
    thumbPadding: Dp = 4.dp,
    checkedTrackColor: Color = RebornTheme.color.grayScale700,
    uncheckedTrackColor: Color = RebornTheme.color.grayScale400,
    thumbColor: Color = Color.White,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        label = "RebornSwitchTrackColor"
    )
    val thumbSize = height - thumbPadding * 2
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - thumbPadding else thumbPadding,
        label = "RebornSwitchThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) trackColor else trackColor.copy(alpha = 0.4f))
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
