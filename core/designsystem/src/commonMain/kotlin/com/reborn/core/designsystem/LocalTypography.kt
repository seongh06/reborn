package com.reborn.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalTypography = staticCompositionLocalOf<RebornTypography> {
    error("RebornTypography가 제공되지 않았습니다. RebornTheme을 적용했는지 확인해주세요.")
}