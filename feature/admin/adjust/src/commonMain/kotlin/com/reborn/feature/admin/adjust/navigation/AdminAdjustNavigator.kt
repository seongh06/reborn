package com.reborn.feature.admin.adjust.navigation

import androidx.compose.foundation.layout.PaddingValues
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
    innerPadding: PaddingValues,
) {
    composable<Route.Admin.Adjust> {
        AdminAdjustRoute(

        )
    }
}