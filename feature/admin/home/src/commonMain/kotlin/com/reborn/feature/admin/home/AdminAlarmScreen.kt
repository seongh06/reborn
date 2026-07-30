package com.reborn.feature.admin.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.home.model.AdminHomeUiState
import com.reborn.feature.admin.home.model.filteredGroupedAlarms

@Composable
fun AdminAlarmScreen(
    state: AdminHomeUiState.Alarm,
    onBackClick: () -> Unit,
    onFilterClick: (AdminHomeUiState.AlarmFilter) -> Unit,
    onAlarmDelete: (Int) -> Unit,
    onAlarmClick: (Int) -> Unit = {}
) {
    val groupedAlarms = state.filteredGroupedAlarms()

    Column(
        modifier = Modifier.rebornDefault(Color.White)
    ) {
        RebornTopAppBar(title = "알림", onBackClick = onBackClick)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminHomeUiState.AlarmFilter.entries.forEach { filter ->
                AlarmFilterChip(
                    label = filter.label,
                    selected = filter == state.filter,
                    onClick = { onFilterClick(filter) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (groupedAlarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "알림이 없습니다",
                            style = RebornTheme.typography.bodyMedium,
                            color = RebornTheme.color.grayScale500
                        )
                    }
                }
            } else {
                groupedAlarms.forEach { (group, items) ->
                    item(key = "group_${group.name}") {
                        Text(
                            text = group.label,
                            style = RebornTheme.typography.labelLarge,
                            color = RebornTheme.color.grayScale600,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(items = items, key = { it.id }) { alarm ->
                        SwipeToDeleteAlarmItem(
                            alarm = alarm,
                            onDelete = { onAlarmDelete(alarm.id) },
                            onClick = { onAlarmClick(alarm.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) {
                    Modifier.background(RebornTheme.color.grayScale300)
                } else {
                    Modifier.border(1.dp, RebornTheme.color.grayScale400, RoundedCornerShape(8.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = RebornTheme.typography.labelLarge,
            color = if (selected) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale600
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteAlarmItem(
    alarm: AdminHomeUiState.AlarmItem,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
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
        AlarmItemRow(alarm = alarm, onClick = onClick)
    }
}

@Composable
private fun AlarmItemRow(alarm: AdminHomeUiState.AlarmItem, onClick: () -> Unit = {}) {
    // 피드백 알림만 상세 화면이 있음 - 결산(SETTLEMENT) 타입은 아직 이동할 대상이 없어 클릭 비활성.
    val isClickable = alarm.category == AdminHomeUiState.AlarmFilter.FEEDBACK
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RebornTheme.color.grayScale300)
            )
            if (alarm.title != null) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = alarm.title,
                        style = RebornTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = RebornTheme.color.grayScale900
                    )
                    Text(
                        text = alarm.content,
                        style = RebornTheme.typography.caption,
                        color = RebornTheme.color.grayScale900
                    )
                    alarm.time?.let { time ->
                        Text(
                            text = time,
                            style = RebornTheme.typography.caption,
                            color = RebornTheme.color.grayScale900
                        )
                    }
                }
            } else {
                Text(
                    text = alarm.content,
                    style = RebornTheme.typography.bodyLarge,
                    color = RebornTheme.color.grayScale900
                )
            }
        }

        if (alarm.title != null) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(70.dp)
                    .background(RebornTheme.color.grayScale300)
            )
        }
    }
}
