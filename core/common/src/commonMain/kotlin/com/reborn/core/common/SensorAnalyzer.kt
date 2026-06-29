package com.reborn.core.common

data class AnalysisResult(val personCount: Int, val lux: Int)

expect class SensorAnalyzer {
    suspend fun analyze(): AnalysisResult
}
