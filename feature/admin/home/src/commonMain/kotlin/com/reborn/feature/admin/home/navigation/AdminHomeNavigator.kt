package com.reborn.feature.admin.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.home.AdminHomeScreen

fun NavController.navigateAdminHome(navOptions: NavOptions) {
    navigate(route = Route.Admin.Home, navOptions = navOptions)
}

fun NavGraphBuilder.adminHomeNavGraph() {
    composable<Route.Admin.Home> {
        AdminHomeScreen()
    }
}
