package com.reborn.feature.admin.feedback.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.feedback.AdminFeedbackRoute

fun NavController.navigateAdminFeedback(navOptions: NavOptions) {
    navigate(route = Route.Admin.Feedback(), navOptions = navOptions)
}

fun NavGraphBuilder.adminFeedbackNavGraph(
    onBackClick: () -> Unit,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
) {
    composable<Route.Admin.Feedback> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Admin.Feedback>()
        AdminFeedbackRoute(
            initialFeedbackId = route.feedbackId,
            initialOpenQr = route.openQr,
            onBackClick = onBackClick,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange
        )
    }
}
