package com.reborn.feature.intro.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.intro.IntroRoute
import com.reborn.feature.intro.IntroScreen

fun NavController.navigateIntro(navOptions: NavOptions) {
    navigate(route = Route.Intro, navOptions = navOptions)
}

fun NavGraphBuilder.introNavGraph(
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    onBackClick: () -> Unit
) {
    composable<Route.Intro> {
        IntroRoute(
            onNavigateToAdmin = onNavigateToAdmin,
            onNavigateToAerometer = onNavigateToAerometer,
            onBackClick =onBackClick
        )
    }
}