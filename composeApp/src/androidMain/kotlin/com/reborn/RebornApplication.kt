package com.reborn

import android.app.Application

class RebornApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
