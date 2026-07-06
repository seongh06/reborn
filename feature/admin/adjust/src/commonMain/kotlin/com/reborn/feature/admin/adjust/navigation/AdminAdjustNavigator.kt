package com.reborn.feature.admin.adjust.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.AdminAddDeviceScreen
import com.reborn.feature.admin.adjust.AdminAdjustRoute
import com.reborn.feature.admin.adjust.AdminAdjustViewModel
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import org.koin.compose.viewmodel.koinViewModel

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
// 제출 시 AdminAdjustScreen 내부 흐름(AdminAdjustIntent.AddDevice)과 동일한 저장 로직을 재사용한다.
fun NavGraphBuilder.adminAddDeviceNavGraph(
    onBackClick: () -> Unit
) {
    composable<Route.Admin.AddDevice> {
        val viewModel: AdminAdjustViewModel = koinViewModel()
        AdminAddDeviceScreen(
            onBackClick = onBackClick,
            onSubmit = { place, name ->
                viewModel.onIntent(AdminAdjustIntent.AddDevice(place, name))
                onBackClick()
            }
        )
    }
}