package com.reborn.core.common

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformSensorModule: Module = module {
    single { SensorAnalyzer(androidContext()) }
}
