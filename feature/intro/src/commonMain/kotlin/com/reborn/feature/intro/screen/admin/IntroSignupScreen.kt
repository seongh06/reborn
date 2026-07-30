package com.reborn.feature.intro.screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.intro.IntroEvent
import com.reborn.feature.intro.IntroViewModel
import com.reborn.feature.intro.component.PlaceTypeList
import com.reborn.feature.intro.model.PlaceType
import org.koin.compose.viewmodel.koinViewModel

// 기존 AdminModeSelect(신규/초대 선택)+AdminPlaceName(이름 입력)+AdminPlaceSelect(유형 선택)
// 3개 화면을 하나로 통합한 Signup 화면(#160) - 이름+유형을 함께 입력받아 "시작하기"로 한 번에 등록한다.
@Composable
fun IntroSignupScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    onInviteCodeClick: () -> Unit,
    viewModel: IntroViewModel = koinViewModel()
) {
    var placeName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<PlaceType?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is IntroEvent.PlaceRegistered -> onNextClick()
                is IntroEvent.ShowErrorSnackbar -> isRegistering = false
                is IntroEvent.NavigateToAdmin,
                is IntroEvent.NavigateToAerometer,
                is IntroEvent.ExitIntro,
                is IntroEvent.LoginSuccess,
                is IntroEvent.AdminCodeIssued,
                is IntroEvent.PairingCodeIssued,
                is IntroEvent.InviteCodeVerified,
                is IntroEvent.InviteCodeInvalid,
                is IntroEvent.DevicePaired -> {}
            }
        }
    }

    if (isRegistering) {
        RebornLoadingScreen()
        return
    }

    Column(
        modifier = Modifier.rebornDefault(RebornTheme.color.grayScale200)
    ) {
        RebornTopAppBar(onBackClick = { onBackClick() })

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "초대 코드 입력하기",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale700,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onInviteCodeClick)
                    .padding(16.dp, 8.dp)
            )

            Text(
                text = "장소 이름",
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900,
                modifier = Modifier.padding(24.dp, 8.dp, 24.dp, 0.dp)
            )
            RebornTextField(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                value = placeName,
                onValueChange = { placeName = it },
                hint = "장소 이름 입력",
                maxLength = 255 // DB place.name VARCHAR(255) — 넘으면 저장 시 서버 오류(#149 참고)
            )

            Text(
                text = "공간 유형 선택",
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900,
                modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 0.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                PlaceType.entries.forEach { type ->
                    PlaceTypeList(
                        placeType = type,
                        selected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }
            }
        }

        RebornButton(
            text = "시작하기",
            enabled = placeName.isNotBlank() && selectedType != null,
            onClick = {
                val type = selectedType
                if (type != null) {
                    isRegistering = true
                    viewModel.registerPlace(placeName, type.name)
                }
            }
        )
    }
}
