package com.reborn

import com.reborn.core.common.platformSensorModule
import com.reborn.core.data.di.repositoryModule
import com.reborn.core.datastore.di.dataStoreModule
import com.reborn.core.datastore.di.platformDataStoreModule
import com.reborn.core.domain.usecase.ConfigureDeviceWifiUseCase
import com.reborn.core.domain.usecase.ControlDeviceUseCase
import com.reborn.core.domain.usecase.DeleteDeviceUseCase
import com.reborn.core.domain.usecase.DeletePlaceUseCase
import com.reborn.core.domain.usecase.LeavePlaceUseCase
import com.reborn.core.domain.usecase.TransferPlaceOwnerUseCase
import com.reborn.core.domain.usecase.GenerateAdminCodeUseCase
import com.reborn.core.domain.usecase.ExportMetricToSheetsUseCase
import com.reborn.core.domain.usecase.GeneratePairingCodeUseCase
import com.reborn.core.domain.usecase.GetAnalysisTextUseCase
import com.reborn.core.domain.usecase.GetAutoControlRuleUseCase
import com.reborn.core.domain.usecase.GetGoogleSheetsAuthorizeUrlUseCase
import com.reborn.core.domain.usecase.GetCurrentMetricUseCase
import com.reborn.core.domain.usecase.GetDeviceListUseCase
import com.reborn.core.domain.usecase.GetDeviceStatusUseCase
import com.reborn.core.domain.usecase.GetFeedbackListUseCase
import com.reborn.core.domain.usecase.GetLocalDeviceIdUseCase
import com.reborn.core.domain.usecase.GetPlaceAdminsUseCase
import com.reborn.core.domain.usecase.GetPlaceDetailUseCase
import com.reborn.core.domain.usecase.GetPlaceListUseCase
import com.reborn.core.domain.usecase.GetPlaceWifiUseCase
import com.reborn.core.domain.usecase.GetSensorAggregateUseCase
import com.reborn.core.domain.usecase.GetSensorHistoryUseCase
import com.reborn.core.domain.usecase.GetSmartThingsAuthorizeUrlUseCase
import com.reborn.core.domain.usecase.GetSmartThingsDeviceListUseCase
import com.reborn.core.domain.usecase.GetTutorialSeenStepsUseCase
import com.reborn.core.domain.usecase.GetUserProfileUseCase
import com.reborn.core.domain.usecase.LoginUseCase
import com.reborn.core.domain.usecase.LogoutUseCase
import com.reborn.core.domain.usecase.PairDeviceUseCase
import com.reborn.core.domain.usecase.RedeemAdminCodeUseCase
import com.reborn.core.domain.usecase.RegisterAiSpeakerDeviceUseCase
import com.reborn.core.domain.usecase.RegisterArduinoDeviceUseCase
import com.reborn.core.domain.usecase.RegisterPlaceUseCase
import com.reborn.core.domain.usecase.RegisterSmartThingsDeviceUseCase
import com.reborn.core.domain.usecase.SaveAutoControlRuleUseCase
import com.reborn.core.domain.usecase.SendMetricUseCase
import com.reborn.core.domain.usecase.MarkTutorialStepSeenUseCase
import com.reborn.core.domain.usecase.UpdateFcmTokenUseCase
import com.reborn.core.domain.usecase.UpdateFeedbackStatusUseCase
import com.reborn.core.domain.usecase.UpdatePlaceWifiUseCase
import com.reborn.core.domain.usecase.UpdateUserProfileImageUseCase
import com.reborn.core.domain.usecase.UpdateUserProfileUseCase
import com.reborn.core.domain.usecase.WithdrawUseCase
import com.reborn.core.network.di.dataSourceModule
import com.reborn.core.network.di.networkModule
import com.reborn.feature.admin.adjust.AdminAdjustViewModel
import com.reborn.feature.admin.data.AdminDataViewModel
import com.reborn.feature.admin.feedback.AdminFeedbackViewModel
import com.reborn.feature.admin.home.AdminHomeViewModel
import com.reborn.feature.admin.home.AdminIotDeviceListViewModel
import com.reborn.feature.admin.home.AdminSmartThingsAddViewModel
import com.reborn.feature.admin.setting.AdminAddAiSpeakerViewModel
import com.reborn.feature.admin.setting.AdminAddArduinoViewModel
import com.reborn.feature.admin.setting.AdminDeviceWifiSetupViewModel
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
    factory { GetTutorialSeenStepsUseCase(get()) }
    factory { MarkTutorialStepSeenUseCase(get()) }
    factory { GetUserProfileUseCase(get()) }
    factory { UpdateUserProfileUseCase(get()) }
    factory { UpdateUserProfileImageUseCase(get()) }
    factory { WithdrawUseCase(get()) }
    factory { GetPlaceDetailUseCase(get()) }
    factory { GetPlaceAdminsUseCase(get()) }
    factory { GetPlaceWifiUseCase(get()) }
    factory { UpdatePlaceWifiUseCase(get()) }
    factory { DeletePlaceUseCase(get()) }
    factory { LeavePlaceUseCase(get()) }
    factory { TransferPlaceOwnerUseCase(get()) }
    factory { GeneratePairingCodeUseCase(get()) }
    factory { PairDeviceUseCase(get()) }
    factory { GetDeviceListUseCase(get()) }
    factory { GetDeviceStatusUseCase(get()) }
    factory { GetLocalDeviceIdUseCase(get()) }
    factory { RegisterArduinoDeviceUseCase(get()) }
    factory { RegisterAiSpeakerDeviceUseCase(get()) }
    factory { GetSmartThingsAuthorizeUrlUseCase(get()) }
    factory { GetSmartThingsDeviceListUseCase(get()) }
    factory { RegisterSmartThingsDeviceUseCase(get()) }
    factory { ConfigureDeviceWifiUseCase(get()) }
    factory { ControlDeviceUseCase(get()) }
    factory { DeleteDeviceUseCase(get()) }
    factory { SaveAutoControlRuleUseCase(get()) }
    factory { GetAutoControlRuleUseCase(get()) }
    factory { GetFeedbackListUseCase(get()) }
    factory { UpdateFeedbackStatusUseCase(get()) }
    factory { GetCurrentMetricUseCase(get()) }
    factory { SendMetricUseCase(get()) }
    factory { GetSensorHistoryUseCase(get()) }
    factory { GetSensorAggregateUseCase(get()) }
    factory { GetAnalysisTextUseCase(get()) }
    factory { ExportMetricToSheetsUseCase(get()) }
    factory { GetGoogleSheetsAuthorizeUrlUseCase(get()) }

    viewModelOf(::IntroViewModel)
    viewModelOf(::AdminHomeViewModel)
    viewModelOf(::AdminSmartThingsAddViewModel)
    viewModelOf(::AdminIotDeviceListViewModel)
    viewModelOf(::AdminAdjustViewModel)
    viewModelOf(::AdminFeedbackViewModel)
    viewModelOf(::AdminDataViewModel)
    viewModelOf(::AdminSettingViewModel)
    viewModelOf(::AdminAddArduinoViewModel)
    viewModelOf(::AdminAddAiSpeakerViewModel)
    viewModelOf(::AdminDeviceWifiSetupViewModel)
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