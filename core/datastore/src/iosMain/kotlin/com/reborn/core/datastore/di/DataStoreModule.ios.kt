package com.reborn.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import com.reborn.core.datastore.AuthTokens
import com.reborn.core.datastore.AuthTokensSerializer
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

private const val AUTH_TOKENS_FILE_NAME = "auth_tokens.pb"

actual val platformDataStoreModule: Module = module {
    single<DataStore<AuthTokens>> {
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = AuthTokensSerializer,
                producePath = { "${NSHomeDirectory()}/$AUTH_TOKENS_FILE_NAME".toPath() },
            ),
        )
    }
}
