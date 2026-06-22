package com.reborn.core.designsystem.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun RebornTheme(
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val currentDensity = LocalDensity.current

        val standardWidth = 360f

        val fixedDensityValue = (maxWidth.value / standardWidth) * currentDensity.density

        val fixedDensity = Density(
            density = fixedDensityValue,
            fontScale = currentDensity.fontScale
        )

        val colors: RebornColor = RebornColor
        val typography = rebornTypography

        CompositionLocalProvider(
            LocalColor provides colors,
            LocalDensity provides fixedDensity,
            LocalTypography provides typography
        ) {
            content()
        }
    }
}



object RebornTheme {
    val color: RebornColor
        @Composable
        get() = LocalColor.current

    val typography: RebornTypography
        @Composable
        get() = LocalTypography.current
}