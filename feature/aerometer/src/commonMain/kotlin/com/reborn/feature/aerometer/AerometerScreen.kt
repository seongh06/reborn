package com.reborn.feature.aerometer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.common.rememberToast
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.aerometer.model.AerometerIntent
import com.reborn.feature.aerometer.model.AerometerUiState
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AeromterRoute(
    viewModel: AerometerViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val showToast = rememberToast()

    LaunchedEffect(Unit) {
        viewModel.onIntent(AerometerIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AerometerEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AerometerEvent.ShowSensorResult -> {
                    showToast("인원: ${event.personCount}명 | 조도: ${event.lux} lux")
                }
                is AerometerEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when (uiState) {
            is AerometerUiState.Loading -> RebornLoadingScreen()
            is AerometerUiState.Home -> AerometerScreen(
                onSettingClick = { viewModel.onIntent(AerometerIntent.NavigateToSetting) }
            )
            is AerometerUiState.Setting -> AerometerSettingScreen(
                onBackClick = { viewModel.onIntent(AerometerIntent.NavigateBack) }
            )
        }
    }
}

@Composable
fun AerometerScreen(
    onSettingClick: () -> Unit,
    viewModel: AerometerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = if (dotCount == 3) 1 else dotCount + 1
        }
    }
    val dots = ".".repeat(dotCount)

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale900),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RebornTopAppBar(onNavigateSetting = onSettingClick, darkTheme = true)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "데이터 수집 중 $dots",
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale100
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
