package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

// Figma(node 595:5149) 기준 - 라벨은 titleSmall/grayScale700, 본문은 bodyLarge/grayScale900,
// 하단에 "AI로 생성된 문자입니다." 고지 문구가 항상 붙는다
@Composable
fun AnalysisResultSection(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "분석 결과",
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale700
        )
        Text(
            text = text,
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale900
        )
        Text(
            text = "AI로 생성된 문자입니다.",
            style = RebornTheme.typography.caption,
            color = RebornTheme.color.grayScale900,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
