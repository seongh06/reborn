package com.reborn.feature.aerometer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.reborn.core.navigation.Route
import com.reborn.feature.aerometer.AerometerScreen

fun NavController.navigateAerometer(navOptions: NavOptions) {
    navigate(route = Route.Aerometer, navOptions = navOptions)
}

fun NavGraphBuilder.aerometerNavGraph() {
    composable<Route.Aerometer> {
        AerometerScreen()
    }
}
