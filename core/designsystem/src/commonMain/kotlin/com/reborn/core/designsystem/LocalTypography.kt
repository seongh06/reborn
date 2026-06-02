package com.reborn.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

internal val LocalTypography = staticCompositionLocalOf<RebornTypography> {
    error("EveryLoLTypography가 제공되지 않았습니다. EveryLoLTheme을 적용했는지 확인해주세요.")
}