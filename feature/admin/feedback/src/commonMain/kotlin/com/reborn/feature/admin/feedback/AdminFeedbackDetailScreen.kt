package com.reborn.feature.admin.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.core.ui.ic_humidity
import com.reborn.core.ui.ic_illuminance
import com.reborn.core.ui.ic_people
import com.reborn.core.ui.ic_temperature
import com.reborn.feature.admin.feedback.model.AdminFeedbackUiState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

@Composable
fun AdminFeedbackDetailScreen(
    state: AdminFeedbackUiState.FeedbackDetail,
    onBackClick: () -> Unit,
    onRejectClick: () -> Unit,
    onApproveClick: () -> Unit
){
    val feedbackDetail = state.feedback

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ){
        RebornTopAppBar(title = "피드백 상세보기", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SensorSnapshotRow(feedbackDetail.sensorSnapshot)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        feedbackDetail.title,
                        style = RebornTheme.typography.titleLarge,
                        color = RebornTheme.color.grayScale900
                    )
                    Text(
                        feedbackDetail.submittedAt,
                        style = RebornTheme.typography.caption,
                        color = RebornTheme.color.grayScale900
                    )
                }
                Text(
                    text = feedbackDetail.content,
                    style = RebornTheme.typography.bodyLarge,
                    color = RebornTheme.color.grayScale900
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(RebornTheme.color.grayScale300)
            )
            AiRecommendationSection(feedbackDetail.temperatureAdjustment)
        }

        Row(
            modifier = Modifier.padding(16.dp, 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RebornButton(
                modifier = Modifier.weight(1f),
                text = "거절",
                backgroundColor = RebornTheme.color.grayScale300,
                onClick = onRejectClick
            )
            RebornButton(
                modifier = Modifier.weight(1f),
                text = "승인",
                backgroundColor = RebornTheme.color.grayScale100,
                onClick = onApproveClick
            )
        }
    }
}

// Figma 596:3594 - 접수 시점 센서 스냅샷 4개 칩(온도/습도/조도/재실 인원)
@Composable
private fun SensorSnapshotRow(snapshot: AdminFeedbackUiState.SensorSnapshot) {
    Row(
        modifier = Modifier.padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SensorSnapshotChip(Res.drawable.ic_temperature, RebornTheme.color.temperature, "${snapshot.temperature}°C")
        SensorSnapshotChip(Res.drawable.ic_humidity, RebornTheme.color.humidity, "${snapshot.humidity.toInt()}%")
        SensorSnapshotChip(Res.drawable.ic_illuminance, RebornTheme.color.illuminance, "${snapshot.illuminance}lx")
        SensorSnapshotChip(Res.drawable.ic_people, RebornTheme.color.peopleCount, "${snapshot.peopleCount}명")
    }
}

@Composable
private fun SensorSnapshotChip(icon: DrawableResource, tint: Color, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(12.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = RebornTheme.typography.labelMedium,
            color = RebornTheme.color.grayScale700
        )
    }
}

// Figma 596:3613 - "AI 맞춤 피드백" 추천 조절 안내
@Composable
private fun AiRecommendationSection(adjustment: AdminFeedbackUiState.TemperatureAdjustment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "AI 맞춤 피드백",
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale700
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "사용자님의 피드백과 실내 환경을 분석해서,",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(RebornTheme.color.grayScale300)
                )
                Column {
                    Text(
                        text = "현재 데이터와 피드백을 합쳐서 => IoT 조절",
                        style = RebornTheme.typography.titleSmall,
                        color = RebornTheme.color.grayScale900
                    )
                    Text(
                        text = buildTemperatureAdjustmentText(adjustment),
                        style = RebornTheme.typography.bodyLarge
                    )
                }
            }
            Text(
                text = "승인을 누르면 바로 전송됩니다.",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
        }
    }
}

@Composable
private fun buildTemperatureAdjustmentText(adjustment: AdminFeedbackUiState.TemperatureAdjustment) = buildAnnotatedString {
    withStyle(SpanStyle(color = RebornTheme.color.grayScale900)) {
        append("희망 온도: ${adjustment.before} → ")
    }
    val diff = adjustment.after - adjustment.before
    val suffix = when {
        diff > 0 -> "(${formatDiff(diff)} 증가)"
        diff < 0 -> "(${formatDiff(abs(diff))} 감소)"
        else -> "(유지)"
    }
    withStyle(SpanStyle(color = RebornTheme.color.humidity, fontWeight = FontWeight.Bold)) {
        append("${adjustment.after} $suffix")
    }
}

private fun formatDiff(value: Double): String {
    val rounded = (value * 10).toInt()
    return if (rounded % 10 == 0) "${rounded / 10}" else "${rounded / 10.0}"
}
