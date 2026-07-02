package com.reborn.feature.admin.adjust.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.AdminAdjustRoute

fun NavController.navigateAdminAdjust(navOptions: NavOptions) {
    navigate(route = Route.Admin.Adjust, navOptions = navOptions)
}

fun NavGraphBuilder.adjustNavGraph(
    onBackClick: () -> Unit
) {
    composable<Route.Admin.Adjust> {
        AdminAdjustRoute(
            onBackClick = onBackClick
        )
    }
}