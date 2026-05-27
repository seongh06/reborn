package com.reborn.core.navigation

sealed interface Route {
    data object Intro : Route
    data object Aerometer : Route

    sealed interface Admin : Route {
        data object Home : Admin
        data object Adjust : Admin
        data object Feedback : Admin
        data object Data : Admin
        data object Setting : Admin
    }
}
