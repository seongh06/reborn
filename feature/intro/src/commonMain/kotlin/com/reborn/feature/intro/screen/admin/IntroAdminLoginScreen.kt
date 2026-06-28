package com.reborn.feature.intro.screen.admin

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
import com.reborn.core.common.PermissionHandler
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
import com.reborn.feature.intro.component.SocialLoginButton
import com.reborn.feature.intro.component.SocialType
import com.reborn.feature.intro.component.TermSection
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import com.reborn.feature.intro.model.PermissionItem
import com.reborn.feature.intro.model.TermItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroAdminLoginScreen(
    onLoginClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        SocialLoginButton(
            socialType = SocialType.KAKAO,
            onClick = { onLoginClick() }
        )
        SocialLoginButton(
            socialType = SocialType.GOOGLE,
            onClick = { onLoginClick() }
        )
    }
}
