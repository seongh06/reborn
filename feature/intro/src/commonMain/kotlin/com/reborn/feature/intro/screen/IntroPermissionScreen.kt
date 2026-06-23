package com.reborn.feature.intro.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.common.PermissionType
import com.reborn.core.common.rememberPermissionManager
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.PermissionSection
import com.reborn.feature.intro.component.TermSection
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import com.reborn.feature.intro.model.PermissionItem
import com.reborn.feature.intro.model.TermItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroPermissionScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    val permissions = remember {
        listOf(
            PermissionItem(1, PermissionType.CAMERA, "카메라 정보", "QR 코드 스캔을 위해 카메라 권한이 필요합니다."),
            PermissionItem(2, PermissionType.LOCATION, "위치 정보", "주변 장소 인식을 위해 위치 권한이 필요합니다."),
        )
    }

    val grantedStates = remember { mutableStateMapOf<PermissionType, Boolean>() }

    val permissionHandler = rememberPermissionManager { type, isGranted ->
        grantedStates[type] = isGranted
        if (permissions.all { grantedStates[it.type] == true }) {
            viewModel.onIntent(IntroIntent.PermissionsGranted)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.PermissionGranted -> onNextClick()
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })
        Text(
            text = "권한 부여",
            style = RebornTheme.typography.displayLarge,
            color = RebornTheme.color.grayScale900
        )
        permissions.forEach { permission ->
            PermissionSection(
                title = permission.title,
                content = permission.content,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            text = "권한 설정",
            onClick = {
                permissions.forEach { permission ->
                    permissionHandler.askPermission(permission.type)
                }
            }
        )
    }
}
