package com.reborn.feature.admin.adjust.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.AdminAdjustRoute

fun NavController.navigateAdminAdjust(navOptions: NavOptions) {
    navigate(route = Route.Admin.Adjust(), navOptions = navOptions)
}

fun NavGraphBuilder.adjustNavGraph(
    onBackClick: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
    onTutorialHintChange: (String?) -> Unit = {}
) {
    composable<Route.Admin.Adjust> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.Adjust>()
        AdminAdjustRoute(
            onBackClick = onBackClick,
            initialDeviceId = route.deviceId,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            onTutorialHintChange = onTutorialHintChange
        )
    }
}