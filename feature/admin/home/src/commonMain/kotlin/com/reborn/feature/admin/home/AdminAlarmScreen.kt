package com.reborn.feature.admin.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.model.AdminHomeUiState

@Composable
fun AdminAlarmScreen(
    state: AdminHomeUiState.Alarm,
    onBackClick: () -> Unit,
    onAlarmDelete: (Int) -> Unit,
    onAlarmAllDelete: () -> Unit
) {
    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "알람", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (state.alarm.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "알람이 없습니다",
                            style = RebornTheme.typography.bodyMedium,
                            color = RebornTheme.color.grayScale500
                        )
                    }
                }
            } else {
                items(items = state.alarm, key = { it.id }) { alarm ->
                    SwipeToDeleteAlarmItem(
                        alarm = alarm,
                        onDelete = { onAlarmDelete(alarm.id) }
                    )
                    HorizontalDivider(color = RebornTheme.color.grayScale300)
                }
            }
        }

        RebornButton(
            text = "알람 전체 삭제하기",
            onClick = onAlarmAllDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteAlarmItem(
    alarm: AdminHomeUiState.AlarmItem,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else false
        }
    )

    val bgColor by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd -> RebornTheme.color.reject
            else -> Color.Transparent
        },
        label = "swipe_bg"
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "삭제",
                    style = RebornTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    ) {
        AlarmItem(alarm = alarm, onDelete = onDelete)
    }
}

@Composable
private fun AlarmItem(
    alarm: AdminHomeUiState.AlarmItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.alarmContent,
                style = RebornTheme.typography.bodyMedium,
                color = RebornTheme.color.grayScale900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alarm.time,
                style = RebornTheme.typography.caption,
                color = RebornTheme.color.grayScale500
            )
        }
        Text(
            text = "삭제",
            style = RebornTheme.typography.labelMedium,
            color = RebornTheme.color.reject,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDelete)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
