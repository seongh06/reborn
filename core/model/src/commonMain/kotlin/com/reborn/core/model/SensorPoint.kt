package com.reborn.core.model

data class SensorPoint(
    val date: String, // "yyyyMMdd"
    val hour: Int, // 0~23, 0시부터 1시간 단위
    val value: Double
)
