package com.reborn.core.datastore.di

import com.reborn.core.datastore.TokenLocalDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformDataStoreModule: Module

val dataStoreModule: Module = module {
    single { TokenLocalDataSource(get()) }
}
