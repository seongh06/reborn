package com.reborn.feature.admin.adjust.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.AdminAddDeviceScreen
import com.reborn.feature.admin.adjust.AdminAdjustRoute

fun NavController.navigateAdminAdjust(navOptions: NavOptions) {
    navigate(route = Route.Admin.Adjust, navOptions = navOptions)
}

fun NavGraphBuilder.adjustNavGraph(
    onBackClick: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
) {
    composable<Route.Admin.Adjust> {
        AdminAdjustRoute(
            onBackClick = onBackClick,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange
        )
    }
}

// Setting의 "디바이스 추가" 버튼에서 진입하는 화면 — Adjust 내부 흐름과 별개의 최상위 라우트
fun NavGraphBuilder.adminAddDeviceNavGraph(
    onBackClick: () -> Unit,
    onSubmit: (place: String, name: String) -> Unit
) {
    composable<Route.Admin.AddDevice> {
        AdminAddDeviceScreen(
            onBackClick = onBackClick,
            onSubmit = onSubmit
        )
    }
}