package com.reborn.core.network.di

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.remote.AuthDataSourceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

val dataSourceModule = module {

    includes(platformDataSourceModule)
    single<AuthDataSource> { AuthDataSourceImpl(get()) }
}

expect val platformDataSourceModule: Module
