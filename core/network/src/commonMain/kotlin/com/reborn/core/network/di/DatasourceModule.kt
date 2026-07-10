package com.reborn.core.network.di

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.datasource.DeviceDataSource
import com.reborn.core.network.datasource.MetricDataSource
import com.reborn.core.network.datasource.PlaceDataSource
import com.reborn.core.network.remote.AuthDataSourceImpl
import com.reborn.core.network.remote.DeviceDataSourceImpl
import com.reborn.core.network.remote.MetricDataSourceImpl
import com.reborn.core.network.remote.PlaceDataSourceImpl
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataSourceModule = module {

    includes(platformDataSourceModule)
    single<AuthDataSource> { AuthDataSourceImpl(get(named("auth"))) }
    single<PlaceDataSource> { PlaceDataSourceImpl(get(named("auth"))) }
    single<DeviceDataSource> { DeviceDataSourceImpl(get(named("auth"))) }
    single<MetricDataSource> { MetricDataSourceImpl(get(named("auth"))) }
}

expect val platformDataSourceModule: Module
