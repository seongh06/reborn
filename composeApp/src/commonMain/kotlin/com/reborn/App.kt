package com.reborn

import androidx.compose.runtime.Composable
import com.reborn.core.designsystem.RebornTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        RebornTheme {
            // NavHost goes here
        }
    }
}
