package com.reborn.core.common

import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformSensorModule: Module = module {
    single { SensorAnalyzer() }
}
