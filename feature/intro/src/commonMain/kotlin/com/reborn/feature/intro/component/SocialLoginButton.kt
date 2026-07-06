package com.reborn.feature.intro.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.intro.Res
import com.reborn.feature.intro.*
import org.jetbrains.compose.resources.painterResource

enum class SocialType {
    KAKAO,
    GOOGLE,
}

@Composable
fun SocialLoginButton(
    socialType: SocialType,
    onClick: () -> Unit
) {

    val (containerColor, buttonText) = when (socialType) {
        SocialType.KAKAO -> Color(0xFFFEE500) to "카카오 로그인"
        SocialType.GOOGLE -> RebornTheme.color.grayScale100 to "구글 로그인"
    }

    Button(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = RebornTheme.color.grayScale900
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    when (socialType) {
                        SocialType.KAKAO -> Res.drawable.ic_kakao
                        SocialType.GOOGLE -> Res.drawable.ic_google
                    }
                ),
                modifier = Modifier.size(20.dp),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Text(
                modifier = Modifier.weight(1f),
                text = buttonText,
                style = RebornTheme.typography.labelLarge,
                color = RebornTheme.color.grayScale900,
                textAlign = TextAlign.Center
            )
        }
    }
}
