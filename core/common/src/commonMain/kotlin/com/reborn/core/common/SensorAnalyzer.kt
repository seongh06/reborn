package com.reborn.core.common

data class AnalysisResult(
    val personCount: Int,
    val lux: Int,
    val savedImagePath: String? = null
)

expect class SensorAnalyzer {
    suspend fun analyze(saveImage: Boolean = false): AnalysisResult
}
