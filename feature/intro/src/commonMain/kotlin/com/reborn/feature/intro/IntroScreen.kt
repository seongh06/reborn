package com.reborn.feature.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroScreen(
    onNavigateToAdmin: () -> Unit,
    onNavigateToAerometer: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
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
