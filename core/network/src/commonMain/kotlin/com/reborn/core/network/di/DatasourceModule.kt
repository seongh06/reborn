package com.reborn.core.network.di

import org.koin.core.module.Module
import org.koin.dsl.module

val dataSourceModule = module {

    includes(platformDataSourceModule)
}

expect val platformDataSourceModule: Module
