package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.reborn.core.ui.*
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

    val icon = getFeedbackIcon(type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RebornTheme.color.grayScale100)
            .border(
                width = 1.dp,
                color = RebornTheme.color.grayScale200,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp,12.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                painterResource(icon.icon),
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = icon.color
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text(
                    title,
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
                Text(
                    time,
                    style = RebornTheme.typography.labelMedium,
                    color = RebornTheme.color.grayScale900
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        stateChip(state)
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

data class StateUiStyle(
    val text: String,
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
            Res.drawable.ic_feedback_hot,
            RebornTheme.color.feedbackHot
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
fun getStateColor(state: State): StateUiStyle {
    return when (state) {
        State.WAITING -> StateUiStyle("대기",RebornTheme.color.grayScale500)
        State.REJECT -> StateUiStyle("거절",RebornTheme.color.reject)
        State.APPROVE -> StateUiStyle("승인",RebornTheme.color.approve)
    }
}

@Composable
fun stateChip(
    state: State
){
    val style = getStateColor(state)

    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(style.color)
            .padding(12.dp,4.dp),
        text = style.text,
        style = RebornTheme.typography.labelMedium,
        color = RebornTheme.color.grayScale100
    )
}