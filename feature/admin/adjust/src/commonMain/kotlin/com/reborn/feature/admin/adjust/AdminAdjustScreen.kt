package com.reborn.feature.admin.adjust

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.Dashboard
import com.reborn.core.ui.component.FeedbackItem
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.State
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.adjust.model.AdminAdjustIntent
import com.reborn.feature.admin.adjust.model.AdminAdjustUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminAdjustRoute(
    viewModel: AdminAdjustViewModel = koinViewModel(),
    onBackClick: () -> Unit,
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminAdjustIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminAdjustEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminAdjustEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
    ){_ ->
        when(val state = uiState) {
            is AdminAdjustUiState.Loading -> RebornLoadingScreen()
            is AdminAdjustUiState.Adjust -> AdminAdjustScreen(
                onAddDeviceClick = {viewModel.onIntent(AdminAdjustIntent.AddDevice)}
            )
        }
    }
}

@Composable
fun AdminAdjustScreen(
    onAddDeviceClick: () -> Unit
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "HOME", onNavigateAddDevice = onAddDeviceClick)
        Dashboard("거실",20,20,20,20)
    }
}
