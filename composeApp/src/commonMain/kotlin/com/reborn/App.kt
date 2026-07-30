package com.reborn

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.scale
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
import com.reborn.core.ui.component.TutorialHintCard
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
import com.reborn.feature.intro.navigation.termsNavGraph
import moe.tlaster.precompose.PreComposeApp
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(initialFeedbackId: Int? = null) {
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
            // 최초 접속 튜토리얼(#240)이 떠 있는 동안엔 바텀 네비 자리에 이 문구 카드가 대신 뜬다.
            var tutorialHintText by remember { mutableStateOf<String?>(null) }

            val surfaceColor = RebornTheme.color.grayScale100
            val scrimColor = RebornTheme.color.grayScale200
            val pillShape = RoundedCornerShape(percent = 50)

            // 태블릿처럼 화면이 넓으면 폰 전용으로 설계된 레이아웃이 그대로 늘어나 UI가 과도하게
            // 커 보여서(#218), 폰 폭을 넘는 영역은 레터박스 처리하고 콘텐츠는 가운데 고정폭으로 유지
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RebornTheme.color.grayScale900),
                contentAlignment = Alignment.TopCenter
            ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
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
                    val isTerms = currentDestination?.hasRoute<Route.Terms>() == true
                    val isSubScreenWithoutBottomBar = isIntro || isAerometer || isAdminSetting ||
                        isAdminInviteCode || isAdminAddDevice || isAdminAddArduino || isAdminDeviceWifiSetup ||
                        isAdminAddAiSpeaker || isAdminIotDeviceList || isAdminAddSmartThingsDevice ||
                        isAdminFeedback || isTerms
                    val isHomeLikeTabHidden = (isAdminHome || isAdminAdjust) &&
                        !isAdminHomeBottomBarVisible
                    // 최초 접속 튜토리얼(#240)이 떠 있는 동안엔 바텀 네비 대신 이 자리에 설명
                    // 카드를 보여준다 - 튜토리얼 중엔 다른 탭으로 이동할 수 없게 해서 집중시킴.
                    if (tutorialHintText != null) {
                        TutorialHintCard(
                            text = tutorialHintText.orEmpty(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else if (!isSubScreenWithoutBottomBar && !isHomeLikeTabHidden) {
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

                                    // One UI 스타일 탭 전환: 선택된 탭 아이콘이 살짝 커졌다가 바운스되며
                                    // 자리잡는 효과 - 눌렀을 때뿐 아니라 다른 진입 경로(딥링크 등)로
                                    // 탭이 바뀌어도 동일하게 애니메이션되도록 isSelected를 그대로 트리거로 사용.
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.15f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "bottomNavIconScale"
                                    )

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
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(iconScale),
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
                    modifier = Modifier.fillMaxSize(),
                    // One UI 스타일 화면 전환: 페이드 + 미세한 수직 슬라이드. 화면 전체를 슬라이드시키면
                    // 무거워 보여서 이동량은 최소화(rebornDefault 등 화면 높이의 1/20 정도)하고 페이드를 주로 사용.
                    enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 20 } },
                    exitTransition = { fadeOut(tween(180)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 20 } }
                ) {
                    introNavGraph(
                        onNavigateToAdmin = {
                            introSkipToSignup = false
                            // FCM 알림 탭으로 콜드/웜 스타트된 경우, 로그인 확인 직후 Home 대신
                            // 바로 해당 피드백 상세로 딥링크(#177 Feedback 딥링크와 동일 패턴)
                            val destination = initialFeedbackId?.let { Route.Admin.Feedback(it) }
                                ?: Route.Admin.Home
                            navController.navigate(destination) {
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
                        onTermsClick = { type ->
                            navController.navigate(Route.Terms(type))
                        },
                        skipToSignup = { introSkipToSignup }
                    )
                    termsNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
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
                        onNavigateToDeviceDetail = { deviceId ->
                            navController.navigate(Route.Admin.Adjust(deviceId))
                        },
                        onNavigateToAddSmartThingsDevice = {
                            navController.navigate(Route.Admin.AddSmartThingsDevice) {
                                launchSingleTop = true
                            }
                        },
                        onBottomBarVisibilityChange = { visible ->
                            isAdminHomeBottomBarVisible = visible
                        },
                        onTutorialHintChange = { text ->
                            tutorialHintText = text
                        }
                    )
                    adminIotDeviceListNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToAddSmartThingsDevice = {
                            navController.navigate(Route.Admin.AddSmartThingsDevice) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToDeviceDetail = { deviceId ->
                            navController.navigate(Route.Admin.Adjust(deviceId))
                        }
                    )
                    adminSmartThingsAddNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                    adjustNavGraph(
                        // IoT 기기 상세 화면에서 뒤로가기는 진입 경로(Home/기기 목록)와 무관하게
                        // 항상 HomeScreen으로 돌아간다(#235) - 바텀 탭 전환과 동일한 패턴 사용.
                        onBackClick = {
                            navController.navigate(Route.Admin.Home) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onBottomBarVisibilityChange = { visible ->
                            isAdminHomeBottomBarVisible = visible
                        },
                        onTutorialHintChange = { text ->
                            tutorialHintText = text
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
                    adminDataNavGraph(
                        onTutorialHintChange = { text ->
                            tutorialHintText = text
                        }
                    )
                    adminSettingNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onTutorialHintChange = { text ->
                            tutorialHintText = text
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
                        onNavigateToTerms = {
                            navController.navigate(Route.Terms())
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
                        onNavigateToWifiSetup = { deviceId, placeId ->
                            navController.navigate(Route.Admin.DeviceWifiSetup(deviceId, placeId))
                        }
                    )
                    adminAddAiSpeakerNavGraph(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onNavigateToWifiSetup = { deviceId, placeId ->
                            navController.navigate(Route.Admin.DeviceWifiSetup(deviceId, placeId))
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
    }
}
