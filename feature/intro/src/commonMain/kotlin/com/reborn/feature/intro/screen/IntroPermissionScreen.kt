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
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.TermSection
import com.reborn.feature.intro.model.IntroIntent
import com.reborn.feature.intro.model.IntroUiState
import com.reborn.feature.intro.model.TermItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IntroPermissionScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val terms = remember {
        listOf(
            TermItem("term1", "이용약관 동의 (필수)", "이용약관 내용..."),
            TermItem("term2", "개인정보 수집 및 이용 동의 (필수)", "개인정보 내용...")
        )
    }
    val checkedTerms = remember { mutableStateMapOf<String, Boolean>() }
    val isAllChecked = terms.all { checkedTerms[it.id] == true }


    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })
        Text(
            text = "개인정보 수집 및 이용 동의",
            style = RebornTheme.typography.displayLarge,
            color = RebornTheme.color.grayScale900
        )
        terms.forEach { term ->
            TermSection(
                title = term.title,
                content = term.content,
                checked = checkedTerms[term.id] ?: false,
                onCheckedChange = { isChecked ->
                    checkedTerms[term.id] = isChecked
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        RebornButton(
            modifier = Modifier.fillMaxWidth(),
            text = "시작하기",
            enabled = isAllChecked,
            onClick = onNextClick
        )
    }
}
