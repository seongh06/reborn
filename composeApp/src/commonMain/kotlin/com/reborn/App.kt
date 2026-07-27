package com.reborn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.reborn.core.designsystem.theme.RebornTheme
import io.ktor.client.HttpClient
import com.reborn.core.navigation.MainTab
import com.reborn.core.navigation.Route
import com.reborn.feature.admin.adjust.navigation.adjustNavGraph
import com.reborn.feature.admin.data.navigation.adminDataNavGraph
import com.reborn.feature.admin.feedback.navigation.adminFeedbackNavGraph
import com.reborn.feature.admin.home.navigation.adminHomeNavGraph
import com.reborn.feature.admin.home.navigation.adminIotDeviceListNavGraph
import com.reborn.feature.admin.home.navigation.adminSmartThingsAddNavGraph
import com.reborn.feature.admin.setting.navigation.adminAddAiSpeakerNavGraph
import com.reborn.feature.admin.setting.navigation.adminAddArduinoNavGraph
import com.reborn.feature.admin.setting.navigation.adminDeviceWifiSetupNavGraph
import com.reborn.feature.admin.setting.navigation.adminSettingNavGraph
import com.reborn.feature.aerometer.navigation.aerometerNavGraph
import com.reborn.feature.intro.navigation.introAdminCodeNavGraph
import com.reborn.feature.intro.navigation.introDevicePairingNavGraph
import com.reborn.feature.intro.navigation.introNavGraph
import moe.tlaster.precompose.PreComposeApp
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    // 프로필 이미지(Kakao/Google CDN) 로딩용 - core:network의 인증된 HttpClient는 우리 API 서버
    // 전용이라 재사용하지 않고, 별도의 플레인 HttpClient로 외부 이미지 호스트에 접근한다(#155).
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { HttpClient() })) }
            .build()
    }

    PreComposeApp {
        val navController = rememberNavController()

        RebornTheme {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            var isAdminHomeBottomBarVisible by remember { mutableStateOf(true) }
            var introSkipToSignup by remember { mutableStateOf(false) }

            val surfaceColor = RebornTheme.color.grayScale100
            val scrimColor = RebornTheme.color.grayScale200
            val pillShape = RoundedCornerShape(percent = 50)

            Scaffold(

                containerColor = RebornTheme.color.grayScale100,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    val isIntro = currentDestination?.hasRoute<Route.Intro>() == true
                    val isAerometer = currentDestination?.hasRoute<Route.Aerometer>() == true
                    val isAdminHome = currentDestination?.hasRoute<Route.Admin.Home>() == true
                    val isAdminAdjust = currentDestination?.hasRoute<Route.Admin.Adjust>() == true
                    val isAdminFeedback = currentDestination?.hasRoute<Route.Admin.Feedback>() == true
                    val isAdminSetting = currentDestination?.hasRoute<Route.Admin.Setting>() == true
                    val isAdminInviteCode = currentDestination?.hasRoute<Route.Admin.InviteCode>() == true
                    val isAdminAddDevice = currentDestination?.hasRoute<Route.Admin.AddDevice>() == true
                    val isAdminAddArduino = currentDestination?.hasRoute<Route.Admin.AddArduino>() == true
                    val isAdminAddAiSpeaker = currentDestination?.hasRoute<Route.Admin.AddAiSpeaker>() == true
                    val isAdminIotDeviceList = currentDestination?.hasRoute<Route.Admin.IotDeviceList>() == true
                    val isAdminAddSmartThingsDevice = currentDestination?.hasRoute<Route.Admin.AddSmartThingsDevice>() == true
                    val isAdminDeviceWifiSetup = currentDestination?.hasRoute<Route.Admin.DeviceWifiSetup>() == true
                    val isSubScreenWithoutBottomBar = isIntro || isAerometer || isAdminSetting ||
                        isAdminInviteCode || isAdminAddDevice || isAdminAddArduino || isAdminDeviceWifiSetup ||
                        isAdminAddAiSpeaker || isAdminIotDeviceList || isAdminAddSmartThingsDevice ||
                        isAdminFeedback
                    val isHomeLikeTabHidden = (isAdminHome || isAdminAdjust) &&
                        !isAdminHomeBottomBarVisible
                    if (!isSubScreenWithoutBottomBar && !isHomeLikeTabHidden) {
                        // Figma BottomNavSection(595:5074) 스펙: 위쪽 투명 -> 아래쪽 불투명 그라데이션
                        // 스크림 위에, 캡슐형(pill) 네비가 가운데 떠 있는 구조.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            scrimColor.copy(alpha = 0f),
                                            scrimColor.copy(alpha = 0.5f),
                                            scrimColor
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp, bottom = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = pillShape,
                                        ambientColor = Color.Black.copy(alpha = 0.25f),
                                        spotColor = Color.Black.copy(alpha = 0.25f)
                                    )
                                    .clip(pillShape)
                                    .background(surfaceColor)
                                    // 안쪽 좌상단 하이라이트 근사 — Figma의 inset shadow(반사광) 대체
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                                        )
                                    )
                                    .padding(horizontal = 8.dp)
                                    .selectableGroup(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val isSelected =
                                        currentDestination?.hasRoute(tab.route::class) == true

                                    // Figma 스펙: 아이템당 64x32 아이콘 컨테이너 + 위 12dp/아래 16dp 여백
                                    // = 아이템 높이 60dp. 2개 탭 * 64dp + Row 좌우 padding(8+8) = 144dp
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 12.dp, bottom = 16.dp)
                                            .size(width = 64.dp, height = 32.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isSelected) tab.selectedIcon else tab.unselectedIcon
                                            ),
                                            modifier = Modifier.size(24.dp),
                                            contentDescription = tab.label,
                                            tint = RebornTheme.color.grayScale700
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { _ ->
                // innerPadding(바텀바 높이만큼)을 그대로 적용하면 콘텐츠가 바텀바 영역까지
                // 아예 안 그려져서, 그라데이션 스크림의 "투명" 부분 뒤에 아무것도 없어 보임
                // (Scaffold의 containerColor만 비침) — edge-to-edge로 깔고 각 화면이 리스트
                // contentPadding으로 하단 여백을 알아서 챙기게 함
                NavHost(
                    navController = navController,
                    startDestination = Route.Intro,
                    modifier = Modifier.fillMaxSize()
                ) {
                    introNavGraph(
                        onNavigateToAdmin = {
                            introSkipToSignup = false
                            navController.navigate(Route.Admin.Home) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onNavigateToAerometer = {
                            introSkipToSignup = false
                            navController.navigate(Route.Aerometer) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onBackClick = {
                            introSkipToSignup = false
                            navController.popBackStack()
                        },
                        skipToSignup = { introSkipToSignup }
                    )
                    introAdminCodeNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    introDevicePairingNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    aerometerNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    adminHomeNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        navigateToFeedbackDetail = { feedbackId ->
                            navController.navigate(Route.Admin.Feedback(feedbackId))
                        },
                        onNavigateToFeedbackList = {
                            navController.navigate(Route.Admin.Feedback())
                        },
                        onNavigateToSetting = {
                            navController.navigate(Route.Admin.Setting)
                        },
                        onNavigateToDeviceList = {
                            navController.navigate(Route.Admin.IotDeviceList)
                        },
                        onBottomBarVisibilityChange = { visible ->
                            isAdminHomeBottomBarVisible = visible
                        }
                    )
                    adminIotDeviceListNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToAddSmartThingsDevice = {
                            navController.navigate(Route.Admin.AddSmartThingsDevice)
                        }
                    )
                    adminSmartThingsAddNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    adjustNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onBottomBarVisibilityChange = { visible ->
                            isAdminHomeBottomBarVisible = visible
                        }
                    )
                    adminFeedbackNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onBottomBarVisibilityChange = { visible ->
                            isAdminHomeBottomBarVisible = visible
                        }
                    )
                    adminDataNavGraph()
                    adminSettingNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToInviteCode = { placeId ->
                            navController.navigate(Route.Admin.InviteCode(placeId))
                        },
                        onNavigateToAddDevice = { placeId ->
                            navController.navigate(Route.Admin.AddDevice(placeId))
                        },
                        onNavigateToAddArduino = { placeId ->
                            navController.navigate(Route.Admin.AddArduino(placeId))
                        },
                        onNavigateToAddAiSpeaker = { placeId ->
                            navController.navigate(Route.Admin.AddAiSpeaker(placeId))
                        },
                        onNavigateToAddPlace = {
                            introSkipToSignup = true
                            navController.navigate(Route.Intro)
                        },
                        onLoggedOut = {
                            introSkipToSignup = false
                            navController.navigate(Route.Intro) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                    adminAddArduinoNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToWifiSetup = { deviceId ->
                            navController.navigate(Route.Admin.DeviceWifiSetup(deviceId))
                        }
                    )
                    adminAddAiSpeakerNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToWifiSetup = { deviceId ->
                            navController.navigate(Route.Admin.DeviceWifiSetup(deviceId))
                        }
                    )
                    adminDeviceWifiSetupNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
