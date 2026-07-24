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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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