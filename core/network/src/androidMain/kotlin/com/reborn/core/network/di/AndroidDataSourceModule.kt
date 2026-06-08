package com.reborn.core.network.di

import android.content.Context
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformDataSourceModule = module {
/*    single { SocialLoginDataSource(get()) }
    single<DataStore<Preferences>> { createDataStore(get<Context>()) }*/
    single<HttpClientEngineFactory<*>> { OkHttp }
    single<Context> { androidContext() }
}