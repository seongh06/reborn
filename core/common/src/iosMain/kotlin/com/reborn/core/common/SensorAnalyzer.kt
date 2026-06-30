package com.reborn.core.common

actual class SensorAnalyzer {
    actual suspend fun analyze(saveImage: Boolean): AnalysisResult = AnalysisResult(0, 0)
}
