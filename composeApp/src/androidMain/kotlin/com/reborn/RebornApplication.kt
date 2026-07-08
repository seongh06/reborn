package com.reborn

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import org.koin.android.ext.koin.androidContext

class RebornApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        initKoin(
            appDeclaration = { androidContext(this@RebornApplication) }
        )
    }
}
