package com.reborn.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.reborn.core.designsystem.*
import org.jetbrains.compose.resources.Font

val Pretendard: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semibold, FontWeight.SemiBold)
    )

@Immutable
data class RebornTypography(
    val displayLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val caption: TextStyle
)

val rebornTypography: RebornTypography
    @Composable
    get() {
        val pretendard = Pretendard
        return RebornTypography(
            //pretendard
            displayLarge = TextStyle(fontFamily = pretendard, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp, letterSpacing = (-0.005).em),
            headlineMedium = TextStyle(fontFamily = pretendard, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp, letterSpacing = (-0.003).em),
            titleLarge = TextStyle(fontFamily = pretendard, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, letterSpacing = (-0.002).em),
            titleMedium = TextStyle(fontFamily = pretendard, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
            titleSmall = TextStyle(fontFamily = pretendard, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontFamily = pretendard, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = (0.001).em),
            bodyMedium = TextStyle(fontFamily = pretendard, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp,letterSpacing = (0.001).em),
            labelLarge = TextStyle(fontFamily = pretendard, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = (0.001).em),
            labelMedium = TextStyle(fontFamily = pretendard, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = (0.002).em),
            caption = TextStyle(fontFamily = pretendard, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
        )
    }
