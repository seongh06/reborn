package com.reborn.feature.admin.data.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.data.AdminDataScreen

fun NavController.navigateAdminData(navOptions: NavOptions) {
    navigate(route = Route.Admin.Data, navOptions = navOptions)
}

fun NavGraphBuilder.adminDataNavGraph() {
    composable<Route.Admin.Data> {
        AdminDataScreen()
    }
}
