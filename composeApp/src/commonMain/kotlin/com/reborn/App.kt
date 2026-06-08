package com.reborn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reborn.core.designsystem.RebornTheme
import com.reborn.core.navigation.MainTab
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.navigation.adjustNavGraph
import com.reborn.feature.admin.data.navigation.adminDataNavGraph
import com.reborn.feature.admin.feedback.navigation.adminFeedbackNavGraph
import com.reborn.feature.admin.home.navigation.adminHomeNavGraph
import com.reborn.feature.admin.setting.navigation.adminSettingNavGraph
import com.reborn.feature.aerometer.navigation.aerometerNavGraph
import com.reborn.feature.intro.navigation.introNavGraph
import moe.tlaster.precompose.PreComposeApp
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext

@Composable
fun App() {
    PreComposeApp {
        val navController = rememberNavController()

        RebornTheme {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            Scaffold(
                containerColor = RebornTheme.color.grayScale100,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    val isIntro = currentDestination?.hasRoute<Route.Intro>() == true

                    if (!isIntro) {
                        Surface(
                            color = RebornTheme.color.grayScale100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    drawLine(
                                        color = RebornTheme.color.grayScale700,
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = strokeWidth
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp)
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val isSelected =
                                        currentDestination?.hasRoute(tab.route::class) == true

                                    NavigationBarItem(
                                        selected = isSelected,
                                        label = {},
                                        icon = {
                                            Icon(
                                                painter = painterResource(
                                                    if (isSelected) tab.selectedIcon else tab.unselectedIcon
                                                ),
                                                modifier = Modifier.size(32.dp),
                                                contentDescription = tab.label
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = RebornTheme.color.grayScale700,
                                            unselectedIconColor = RebornTheme.color.grayScale700,
                                        ),
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Route.Intro,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    introNavGraph(
                        onNavigateToAdmin = {
                            navController.navigate(Route.Admin.Home) {
                                popUpTo(Route.Intro) { inclusive = true }
                            }
                        },
                        onNavigateToAerometer = {
                            navController.navigate(Route.Aerometer) {
                                popUpTo(Route.Intro) { inclusive = true }
                            }
                        }
                    )
                    adminHomeNavGraph()
                    adjustNavGraph()
                    adminFeedbackNavGraph()
                    adminDataNavGraph()
                    adminSettingNavGraph()
                    aerometerNavGraph()
                }
            }
        }
    }
}
