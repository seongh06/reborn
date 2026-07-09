package com.reborn.core.data.di

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.datasource.AuthLocalDataSourceImpl
import com.reborn.core.data.repository.AuthRepositoryImpl
import com.reborn.core.data.repository.PlaceRepositoryImpl
import com.reborn.core.domain.repository.AuthRepository
import com.reborn.core.domain.repository.PlaceRepository
import org.koin.dsl.module


val repositoryModule = module {

    single<AuthLocalDataSource> { AuthLocalDataSourceImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<PlaceRepository> { PlaceRepositoryImpl(get(), get()) }

}