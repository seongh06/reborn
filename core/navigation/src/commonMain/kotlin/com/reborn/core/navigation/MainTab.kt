package com.reborn.core.navigation

import org.jetbrains.compose.resources.DrawableResource

enum class MainTab (
    val route: Route,
    val label: String,
    val selectedIcon: DrawableResource,
    val unselectedIcon: DrawableResource
){
    Home(Route.Admin.Home, "홈", Res.drawable.ic_home_active, Res.drawable.ic_home_unactive),
    Adjust(Route.Admin.Adjust, "조절",Res.drawable.ic_adjust_active, Res.drawable.ic_adjust_unactive),
    Feedback(Route.Admin.Feedback, "피드백", Res.drawable.ic_feedback_active, Res.drawable.ic_feedback_unactive),
    Data(Route.Admin.Data, "데이터", Res.drawable.ic_data_active, Res.drawable.ic_data_unactive)
}