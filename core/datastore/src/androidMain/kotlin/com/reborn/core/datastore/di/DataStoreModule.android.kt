package com.reborn.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import com.reborn.core.datastore.AuthTokens
import com.reborn.core.datastore.AuthTokensSerializer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

private const val AUTH_TOKENS_FILE_NAME = "auth_tokens.pb"

actual val platformDataStoreModule: Module = module {
    single<DataStore<AuthTokens>> {
        val context = androidContext()
        DataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = AuthTokensSerializer,
                producePath = { File(context.filesDir, AUTH_TOKENS_FILE_NAME).toOkioPath() },
            ),
        )
    }
}
