package com.reborn.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Intro : Route
    @Serializable data object Aerometer : Route

    @Serializable
    sealed interface Admin : Route {
        @Serializable data object Home : Admin
        @Serializable data object Adjust : Admin
        @Serializable data object Feedback : Admin
        @Serializable data object Data : Admin
        @Serializable data object Setting : Admin
    }
}