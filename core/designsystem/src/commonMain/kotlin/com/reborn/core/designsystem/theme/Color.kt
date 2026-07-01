package com.reborn.core.designsystem.theme

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

interface RebornColors{
    val grayScale100: Color
    val grayScale200: Color
    val grayScale300: Color
    val grayScale400: Color
    val grayScale500: Color
    val grayScale600: Color
    val grayScale700: Color
    val grayScale800: Color
    val grayScale900: Color
    val temperature: Color
    val temperatureRadial: Brush
    val humidity: Color
    val humidityRadial: Brush
    val illuminance: Color
    val illuminanceRadial: Brush
    val peopleCount: Color
    val peopleCountRadial: Brush
    val feedbackHot: Color
    val feedbackSmell: Color
    val feedbackLight: Color
    val feedbackAir: Color
    val feedbackMusic: Color
    val feedbackNoise: Color
    val feedbackCold: Color
    val feedbackWind: Color
    val feedbackDirt: Color
    val feedbackDark: Color
    val approve: Color
    val reject: Color
}

@Stable
object RebornColor : RebornColors{
    override val grayScale100 = Color(0xFFF9F9F9)
    override val grayScale200 = Color(0xFFF4F4F4)
    override val grayScale300 = Color(0xFFEBEBEB)
    override val grayScale400 = Color(0xFFDBDBDB)
    override val grayScale500 = Color(0xFFA6A6A6)
    override val grayScale600 = Color(0xFF8E8E8E)
    override val grayScale700 = Color(0xFF767676)
    override val grayScale800 = Color(0xFF4A4A4A)
    override val grayScale900 = Color(0xFF121212)
    override val temperature = Color(0xFFD65C5C)
    override val temperatureRadial: Brush = Brush.horizontalGradient(
        colors = listOf(Color(0x47B60000), Color(0x1A000000))
    )
    override val humidity = Color(0xFF5CADD6)
    override val humidityRadial: Brush = Brush.horizontalGradient(
        colors = listOf(Color(0x475CADD6), Color(0x1A000000))
    )
    override val illuminance = Color(0xFFD6C25C)
    override val illuminanceRadial: Brush = Brush.horizontalGradient(
        colors = listOf(Color(0x47F7F7F7), Color(0x1A000000))
    )
    override val peopleCount = Color(0xFF5CD65C)
    override val peopleCountRadial: Brush = Brush.horizontalGradient(
        colors = listOf(Color(0x47F7F7F7), Color(0x1A000000))
    )
    override val feedbackHot = Color(0xFFBF4040)
    override val feedbackSmell = Color(0xFFBF8040)
    override val feedbackLight = Color(0xFFBFBF40)
    override val feedbackAir = Color(0xFF80BF40)
    override val feedbackMusic = Color(0xFF40BF40)
    override val feedbackNoise = Color(0xFF40BF80)
    override val feedbackCold = Color(0xFF40BFBF)
    override val feedbackWind = Color(0xFF406ABF)
    override val feedbackDirt = Color(0xFFBF40BF)
    override val feedbackDark = Color(0xFF808080)
    override val approve = Color(0xFF54B547)
    override val reject = Color(0xFFCE2B2B)
}