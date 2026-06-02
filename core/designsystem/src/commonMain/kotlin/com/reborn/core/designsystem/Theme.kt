package com.reborn.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.reborn.core.designsystem.theme.LocalColor
import com.reborn.core.designsystem.theme.LocalTypography
import com.reborn.core.designsystem.theme.RebornColor
import com.reborn.core.designsystem.theme.RebornTypography
import com.reborn.core.designsystem.theme.rebornTypography

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun RebornTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val currentDensity = LocalDensity.current

        val standardWidth = 360f

        val fixedDensityValue = (maxWidth.value / standardWidth) * currentDensity.density

        val fixedDensity = Density(
            density = fixedDensityValue,
            fontScale = 1f
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