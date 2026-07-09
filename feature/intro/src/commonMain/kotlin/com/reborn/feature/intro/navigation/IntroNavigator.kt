package com.reborn.feature.intro.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.reborn.core.navigation.Route
import com.reborn.feature.intro.IntroRoute
import com.reborn.feature.intro.IntroScreen
import com.reborn.feature.intro.screen.admin.IntroAdminCodeScreen

fun NavController.navigateIntro(navOptions: NavOptions) {
    navigate(route = Route.Intro, navOptions = navOptions)
}

fun NavGraphBuilder.introNavGraph(
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    onBackClick: () -> Unit,
    skipToAdminModeSelect: () -> Boolean = { false }
) {
    composable<Route.Intro> {
        IntroRoute(
            onNavigateToAdmin = onNavigateToAdmin,
            onNavigateToAerometer = onNavigateToAerometer,
            onBackClick =onBackClick,
            skipToAdminModeSelect = skipToAdminModeSelect()
        )
    }
}

// Setting의 "관리자 초대" 버튼에서 진입하는 관리자 초대 코드 화면 — Intro 온보딩 플로우와 별개의 최상위 라우트
fun NavGraphBuilder.introAdminCodeNavGraph(
    onBackClick: () -> Unit
) {
    composable<Route.Admin.InviteCode> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.InviteCode>()
        IntroAdminCodeScreen(
            placeId = route.placeId.toLong(),
            onBackClick = onBackClick
        )
    }
}