package com.reborn.core.data.di

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.datasource.AuthLocalDataSourceImpl
import com.reborn.core.data.datasource.DeviceLocalDataSource
import com.reborn.core.data.datasource.DeviceLocalDataSourceImpl
import com.reborn.core.data.repository.AuthRepositoryImpl
import com.reborn.core.data.repository.DeviceRepositoryImpl
import com.reborn.core.data.repository.PlaceRepositoryImpl
import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.domain.repository.PlaceRepository
import org.koin.dsl.module


val repositoryModule = module {

    single<AuthLocalDataSource> { AuthLocalDataSourceImpl(get()) }
    single<DeviceLocalDataSource> { DeviceLocalDataSourceImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<PlaceRepository> { PlaceRepositoryImpl(get()) }
    single<DeviceRepository> { DeviceRepositoryImpl(get(), get()) }

}