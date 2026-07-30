package com.reborn.feature.admin.data.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.admin.data.model.MIN_ANALYSIS_DATA_COUNT

// Figma(node 595:5149) 기준 - 라벨은 titleSmall/grayScale700, 본문은 bodyLarge/grayScale900.
// AI 분석은 Gemini 호출 비용이 들어서(사용자 요청) 데이터가 충분할 때도 자동으로 부르지 않고,
// 블러 처리된 카드를 탭해야만 실제로 호출한다 - "AI로 생성된 문구입니다." 고지도 실제 AI 응답이
// 온 뒤(text != null)에만 붙는다. 데이터 자체가 부족하면 안내 문구만 보여주고 탭도 막는다.
@Composable
fun AnalysisResultSection(
    dataCount: Int,
    text: String?,
    isLoading: Boolean,
    onRevealClick: () -> Unit,
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
        when {
            dataCount == 0 -> Text(
                text = "아직 데이터가 수집되지 않았습니다.",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale700
            )
            dataCount < MIN_ANALYSIS_DATA_COUNT -> Text(
                text = "아직 수집한 데이터가 적어 분석할 수 없습니다.",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale700
            )
            text != null -> {
                Text(
                    text = text,
                    style = RebornTheme.typography.bodyLarge,
                    color = RebornTheme.color.grayScale900
                )
                Text(
                    text = "AI로 생성된 문구입니다.",
                    style = RebornTheme.typography.caption,
                    color = RebornTheme.color.grayScale900,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RebornTheme.color.grayScale200)
                    .clickable(enabled = !isLoading, onClick = onRevealClick)
            ) {
                // 아직 요청하지 않은 실제 내용은 없으니, 자리만 차지하는 더미 문단을 블러 처리해
                // "여기에 분석 결과가 들어갈 자리"라는 느낌만 준다. 탭 안내/로딩 문구는 블러 없이
                // 그 위에 그대로 얹는다.
                Text(
                    text = "온도와 습도, 조도, 재실 인원 데이터를 종합해 이 공간의 최근 경향을 " +
                        "분석하고 쾌적한 환경을 위한 제안을 정리해드립니다.",
                    style = RebornTheme.typography.bodyLarge,
                    color = RebornTheme.color.grayScale500,
                    modifier = Modifier.blur(10.dp).padding(vertical = 4.dp)
                )
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "분석 중..." else "탭해서 분석 결과 보기",
                        style = RebornTheme.typography.titleSmall,
                        color = RebornTheme.color.grayScale900
                    )
                }
            }
        }
    }
}
