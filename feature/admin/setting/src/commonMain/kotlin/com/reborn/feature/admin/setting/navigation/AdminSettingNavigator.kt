package com.reborn.feature.admin.setting.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.setting.AdminAddAiSpeakerRoute
import com.reborn.feature.admin.setting.AdminAddArduinoRoute
import com.reborn.feature.admin.setting.AdminSettingRoute

fun NavController.navigateAdminSetting(navOptions: NavOptions) {
    navigate(route = Route.Admin.Setting, navOptions = navOptions)
}

fun NavGraphBuilder.adminSettingNavGraph(
    onBackClick: () -> Unit,
    onNavigateToInviteCode: (Int) -> Unit = {},
    onNavigateToAddDevice: (Int) -> Unit = {},
    onNavigateToAddArduino: (Int) -> Unit = {},
    onNavigateToAddAiSpeaker: (Int) -> Unit = {},
    onNavigateToAddPlace: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    composable<Route.Admin.Setting> {
        AdminSettingRoute(
            onBackClick = onBackClick,
            onNavigateToInviteCode = onNavigateToInviteCode,
            onNavigateToAddDevice = onNavigateToAddDevice,
            onNavigateToAddArduino = onNavigateToAddArduino,
            onNavigateToAddAiSpeaker = onNavigateToAddAiSpeaker,
            onNavigateToAddPlace = onNavigateToAddPlace,
            onLoggedOut = onLoggedOut
        )
    }
}

fun NavGraphBuilder.adminAddArduinoNavGraph(
    onBackClick: () -> Unit,
) {
    composable<Route.Admin.AddArduino> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.AddArduino>()
        AdminAddArduinoRoute(
            placeId = route.placeId.toLong(),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.adminAddAiSpeakerNavGraph(
    onBackClick: () -> Unit,
) {
    composable<Route.Admin.AddAiSpeaker> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.AddAiSpeaker>()
        AdminAddAiSpeakerRoute(
            placeId = route.placeId.toLong(),
            onBackClick = onBackClick,
        )
    }
}
