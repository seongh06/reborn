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
import com.reborn.feature.intro.screen.admin.IntroDevicePairingCodeScreen

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

// Setting의 "디바이스 추가" 버튼에서 진입하는 공기계 페어링 코드 화면 — "디바이스 추가"는 실기기(Arduino)가 아니라
// 공기계를 이 장소에 새로 연결하는 것을 의미하므로, 온보딩과 동일한 페어링 코드 발급 화면을 재사용한다.
fun NavGraphBuilder.introDevicePairingNavGraph(
    onBackClick: () -> Unit
) {
    composable<Route.Admin.AddDevice> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.AddDevice>()
        IntroDevicePairingCodeScreen(
            placeId = route.placeId.toLong(),
            onBackClick = onBackClick,
            onNextClick = onBackClick
        )
    }
}