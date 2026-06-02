package com.reborn.feature.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun IntroRoute(
    viewModel: IntroViewModel = koinViewModel(),
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit
){

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(IntroIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is IntroEvent.NavigateToAdmin -> onNavigateToAdmin()
                is IntroEvent.NavigateToAerometer -> onNavigateToAerometer()
            }
        }
    }

    IntroScreen(
        onNavigateToAdmin = { viewModel.onIntent(IntroIntent.NavigateToAdmin) },
        onNavigateToAerometer = { viewModel.onIntent(IntroIntent.NavigateToAerometer) },
        state = uiState
    )

}


@Composable
fun IntroScreen(
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    state : IntroUiState
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onNavigateToAdmin) { Text("Admin") }
        Button(onClick = onNavigateToAerometer) { Text("Aerometer") }
    }
}
