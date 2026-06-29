package com.reborn.core.common

actual class SensorAnalyzer {
    actual suspend fun analyze(): AnalysisResult = AnalysisResult(0, 0)
}
