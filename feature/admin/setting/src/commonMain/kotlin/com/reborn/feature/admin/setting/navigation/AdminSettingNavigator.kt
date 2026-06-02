package com.reborn.feature.admin.setting.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.setting.AdminSettingScreen

fun NavController.navigateAdminSetting(navOptions: NavOptions) {
    navigate(route = Route.Admin.Setting, navOptions = navOptions)
}

fun NavGraphBuilder.adminSettingNavGraph() {
    composable<Route.Admin.Setting> {
        AdminSettingScreen()
    }
}
