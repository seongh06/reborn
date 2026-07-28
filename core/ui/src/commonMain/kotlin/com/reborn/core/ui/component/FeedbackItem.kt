package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.ic_feedback_air
import com.reborn.core.ui.ic_feedback_cold
import com.reborn.core.ui.ic_feedback_dark
import com.reborn.core.ui.ic_feedback_dirt
import com.reborn.core.ui.ic_feedback_hot
import com.reborn.core.ui.ic_feedback_light
import com.reborn.core.ui.ic_feedback_music
import com.reborn.core.ui.ic_feedback_noise
import com.reborn.core.ui.ic_feedback_smell
import com.reborn.core.ui.ic_feedback_wind
import com.reborn.core.ui.ic_none_feedback
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun FeedbackItem(
    type: FeedbackType,
    state: State,
    time: String,
    title: String,
    id: Int,
    onClick:() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                Icon(
                    painter = painterResource(getFeedbackIcon(type).icon),
                    tint = getFeedbackIcon(type).color,
                    modifier = Modifier.size(16.dp),
                    contentDescription = null
                )
                Text(
                    text = time,
                    style = RebornTheme.typography.labelMedium,
                    color = RebornTheme.color.grayScale900
                )
            }
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getStateColor(state))
                    .semantics { stateDescription = getStateLabel(state) }
            )
        }
        Text(
            text = title,
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900
        )
    }
}

data class FeedbackListItem(
    val id: Int,
    val type: FeedbackType,
    val state: State,
    val time: String,
    val title: String
)

@Composable
fun FeedbackList(
    items: List<FeedbackListItem>,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RebornTheme.color.grayScale100),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_none_feedback),
                modifier = Modifier.size(36.dp),
                contentDescription = null,
                tint = RebornTheme.color.grayScale700
            )
            Text(
                text = "현재 피드백이 없습니다",
                style = RebornTheme.typography.bodyLarge,
                color = RebornTheme.color.grayScale900
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(16.dp),
        // 아이템이 min height(120dp)보다 짧을 때(특히 1개일 때) Arrangement.Center를 쓰면
        // 목록이 박스 가운데로 밀려 보임 - 항상 위에서부터 쌓이도록 Top으로 고정
        verticalArrangement = Arrangement.Top
    ) {
        items.forEachIndexed { index, item ->
            Column {
                FeedbackItem(
                    type = item.type,
                    state = item.state,
                    time = item.time,
                    title = item.title,
                    id = item.id,
                    onClick = { onItemClick(item.id) }
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(color = RebornTheme.color.grayScale300, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

enum class FeedbackType {
    HOT, SMELL, LIGHT, AIR, MUSIC, NOISE, COLD, WIND, DIRT, DARK
}

// 서버 status 문자열 <-> UI State 매핑 - Home/Feedback 화면이 공유
fun feedbackStatusToState(status: String): State = when (status) {
    "APPROVED" -> State.APPROVE
    "REJECTED" -> State.REJECT
    else -> State.WAITING
}

// 서버 createdAt(오프셋 없는 LocalDateTime ISO 문자열)을 "n분전" 형태로 변환 - Home/Feedback 화면이 공유
fun formatFeedbackRelativeTime(iso: String): String {
    val createdInstant = LocalDateTime.parse(iso).toInstant(TimeZone.currentSystemDefault())
    val minutes = (Clock.System.now() - createdInstant).inWholeMinutes
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분전"
        minutes < 60 * 24 -> "${minutes / 60}시간전"
        minutes < 60 * 24 * 2 -> "어제"
        else -> "${minutes / (60 * 24)}일전"
    }
}

// 서버가 피드백 유형을 분류해주지 않아(content만 저장) 키워드로 추정한다 -
// 매칭되는 키워드가 없으면 AIR로 기본 처리
fun classifyFeedbackType(content: String): FeedbackType = when {
    content.contains("덥") || content.contains("더워") -> FeedbackType.HOT
    content.contains("춥") || content.contains("추워") -> FeedbackType.COLD
    content.contains("어둡") -> FeedbackType.DARK
    content.contains("밝") -> FeedbackType.LIGHT
    content.contains("냄새") -> FeedbackType.SMELL
    content.contains("먼지") -> FeedbackType.DIRT
    content.contains("바람") || content.contains("환기") -> FeedbackType.WIND
    content.contains("시끄럽") || content.contains("소음") -> FeedbackType.NOISE
    content.contains("음악") || content.contains("소리") -> FeedbackType.MUSIC
    else -> FeedbackType.AIR
}

enum class State {
    WAITING, REJECT, APPROVE
}

data class FeedbackUiStyle(
    val icon: DrawableResource,
    val color: Color
)

@Composable
fun getFeedbackIcon(type: FeedbackType): FeedbackUiStyle {
    return when (type) {
        FeedbackType.HOT -> FeedbackUiStyle(
            Res.drawable.ic_feedback_hot,
            RebornTheme.color.feedbackHot
        )
        FeedbackType.SMELL -> FeedbackUiStyle(
            Res.drawable.ic_feedback_smell,
            RebornTheme.color.feedbackSmell
        )
        FeedbackType.LIGHT -> FeedbackUiStyle(
            Res.drawable.ic_feedback_light,
            RebornTheme.color.feedbackLight
        )
        FeedbackType.AIR -> FeedbackUiStyle(
            Res.drawable.ic_feedback_air,
            RebornTheme.color.feedbackAir
        )
        FeedbackType.MUSIC ->  FeedbackUiStyle(
            Res.drawable.ic_feedback_music,
            RebornTheme.color.feedbackMusic
        )
        FeedbackType.NOISE -> FeedbackUiStyle(
            Res.drawable.ic_feedback_noise,
            RebornTheme.color.feedbackNoise
        )
        FeedbackType.COLD ->  FeedbackUiStyle(
            Res.drawable.ic_feedback_cold,
            RebornTheme.color.feedbackCold
        )
        FeedbackType.WIND ->  FeedbackUiStyle(
            Res.drawable.ic_feedback_wind,
            RebornTheme.color.feedbackWind
        )
        FeedbackType.DIRT ->  FeedbackUiStyle(
            Res.drawable.ic_feedback_dirt,
            RebornTheme.color.feedbackDirt
        )
        FeedbackType.DARK ->  FeedbackUiStyle(
            Res.drawable.ic_feedback_dark,
            RebornTheme.color.feedbackDark
        )
    }
}

@Composable
fun getStateColor(state: State): Color {
    return when (state) {
        State.WAITING -> RebornTheme.color.grayScale500
        State.REJECT -> RebornTheme.color.reject
        State.APPROVE -> RebornTheme.color.approve
    }
}

// 상태를 색상만으로 구분하면 색약 사용자가 인지하기 어려워 스크린리더용 텍스트 대안 제공
fun getStateLabel(state: State): String {
    return when (state) {
        State.WAITING -> "대기"
        State.REJECT -> "거절"
        State.APPROVE -> "승인"
    }
}