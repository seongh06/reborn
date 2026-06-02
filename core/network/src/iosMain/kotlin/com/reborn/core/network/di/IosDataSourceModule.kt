package com.reborn.core.network.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import every.lol.com.core.datastore.createDataStore
import every.lol.com.core.network.datasource.SocialLoginDataSource
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

actual val platformDataSourceModule = module {
    single<DataStore<Preferences>> { createDataStore() }
    single { SocialLoginDataSource() }
    single<HttpClientEngineFactory<*>> { Darwin }
    single<Any>{object {}}
}