package com.reborn.feature.admin.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.home.AdminHomeEvent
import com.reborn.feature.admin.home.AdminHomeRoute
import com.reborn.feature.admin.home.AdminHomeScreen
import com.reborn.feature.admin.home.AdminIotDeviceListRoute
import com.reborn.feature.admin.home.AdminSmartThingsAddRoute

fun NavController.navigateAdminHome(navOptions: NavOptions) {
    navigate(route = Route.Admin.Home, navOptions = navOptions)
}

fun NavGraphBuilder.adminHomeNavGraph(
    onBackClick: () -> Unit,
    navigateToFeedbackDetail: (Int) -> Unit,
    onNavigateToFeedbackList: () -> Unit = {},
    onNavigateToSetting: () -> Unit = {},
    onNavigateToDeviceList: () -> Unit = {},
    onNavigateToDeviceDetail: (String) -> Unit = {},
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
) {
    composable<Route.Admin.Home> {
        AdminHomeRoute(
            onBackClick = onBackClick,
            navigateToFeedbackDetail = navigateToFeedbackDetail,
            onNavigateToFeedbackList = onNavigateToFeedbackList,
            onNavigateToSetting = onNavigateToSetting,
            onNavigateToDeviceList = onNavigateToDeviceList,
            onNavigateToDeviceDetail = onNavigateToDeviceDetail,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange
        )
    }
}

fun NavGraphBuilder.adminIotDeviceListNavGraph(
    onBackClick: () -> Unit,
    onNavigateToAddSmartThingsDevice: () -> Unit = {},
    onNavigateToDeviceDetail: (String) -> Unit = {}
) {
    composable<Route.Admin.IotDeviceList> {
        AdminIotDeviceListRoute(
            onBackClick = onBackClick,
            onAddDeviceClick = onNavigateToAddSmartThingsDevice,
            onDeviceClick = onNavigateToDeviceDetail
        )
    }
}

fun NavGraphBuilder.adminSmartThingsAddNavGraph(
    onBackClick: () -> Unit
) {
    composable<Route.Admin.AddSmartThingsDevice> {
        AdminSmartThingsAddRoute(onBackClick = onBackClick)
    }
}
