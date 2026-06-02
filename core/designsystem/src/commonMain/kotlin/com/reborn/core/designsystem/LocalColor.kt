package com.reborn.core.designsystem.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.reborn.core.designsystem.theme.RebornColor

internal val LocalColor = staticCompositionLocalOf<RebornColor> {
    error("LocalColor가 제공되지 않았습니다. EveryLoLTheme을 적용했는지 확인해주세요.")
}