package com.reborn

import android.app.Application
import org.koin.android.ext.koin.androidContext

class RebornApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            appDeclaration = { androidContext(this@RebornApplication) }
        )
    }
}
