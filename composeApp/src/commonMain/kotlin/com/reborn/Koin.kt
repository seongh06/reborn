package com.reborn

import com.reborn.core.common.platformSensorModule
import com.reborn.core.data.di.repositoryModule
import com.reborn.core.datastore.di.dataStoreModule
import com.reborn.core.datastore.di.platformDataStoreModule
import com.reborn.core.domain.usecase.DeletePlaceUseCase
import com.reborn.core.domain.usecase.GenerateAdminCodeUseCase
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.LoginUseCase
import com.reborn.core.domain.usecase.LogoutUseCase
import com.reborn.core.domain.usecase.RedeemAdminCodeUseCase
import com.reborn.core.domain.usecase.RegisterPlaceUseCase
import com.reborn.core.domain.usecase.UpdateFcmTokenUseCase
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
    factory { LoginUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { UpdateFcmTokenUseCase(get()) }
    factory { RegisterPlaceUseCase(get()) }
    factory { GenerateAdminCodeUseCase(get()) }
    factory { RedeemAdminCodeUseCase(get()) }
    factory { GetPlaceListUseCase(get()) }
    factory { GetPlaceDetailUseCase(get()) }
    factory { DeletePlaceUseCase(get()) }

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
            dataStoreModule,
            platformDataStoreModule,
            appDependenciesModule,
            platformSensorModule,
            *platformModules.toTypedArray()
        )
    }
}

object KoinBridge {
    fun start() {
        initKoin()
    }
}