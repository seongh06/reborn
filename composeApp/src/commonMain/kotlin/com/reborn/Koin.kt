package com.reborn

import com.reborn.core.data.di.repositoryModule
import com.reborn.core.network.di.dataSourceModule
import com.reborn.core.network.di.networkModule
import com.reborn.feature.admin.adjust.AdminAdjustViewModel
import com.reborn.feature.admin.data.AdminDataViewModel
import com.reborn.feature.admin.feedback.AdminFeedbackViewModel
import com.reborn.feature.admin.home.AdminHomeViewModel
import com.reborn.feature.admin.setting.AdminSettingViewModel
import com.reborn.feature.aerometer.AerometerViewModel
import com.reborn.feature.intro.IntroViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module


val appDependenciesModule = module {
    viewModelOf(::IntroViewModel)
    viewModelOf(::AdminHomeViewModel)
    viewModelOf(::AdminAdjustViewModel)
    viewModelOf(::AdminFeedbackViewModel)
    viewModelOf(::AdminDataViewModel)
    viewModelOf(::AdminSettingViewModel)
    viewModelOf(::AerometerViewModel)
}
fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
    platformModules: List<Module> = emptyList()
) {
    startKoin {
        appDeclaration()
        printLogger(Level.DEBUG)
        modules(
            networkModule,
            dataSourceModule,
            repositoryModule,
            appDependenciesModule,
            *platformModules.toTypedArray()
        )
    }
}

object KoinBridge {
    fun start() {
        initKoin()
    }
}