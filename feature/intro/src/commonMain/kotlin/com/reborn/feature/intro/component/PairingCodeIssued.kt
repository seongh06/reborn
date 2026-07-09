package com.reborn.feature.intro.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.intro.Res
import com.reborn.feature.intro.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun PairingCodeIssued(
    pairingCode: String,
    initialSeconds: Int
) {
    val minutes = initialSeconds / 60
    val seconds = initialSeconds % 60
    val timeString = "${minutes}:${seconds.toString().padStart(2, '0')}"

    Column(
        modifier = Modifier.padding(16.dp,8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = pairingCode,
            style = RebornTheme.typography.displayLarge,
            color = RebornTheme.color.grayScale900
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(

                text = "제한 시간: $timeString",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
            Text(
                text = "해당 코드를 조작용 휴대폰에 입력해주세요",
                style = RebornTheme.typography.bodyMedium,
                color = RebornTheme.color.grayScale900
            )
        }
    }
}