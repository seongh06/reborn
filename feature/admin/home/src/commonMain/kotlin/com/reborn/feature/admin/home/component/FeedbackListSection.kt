package com.reborn.feature.admin.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reborn.core.ui.component.FeedbackList
import com.reborn.core.ui.component.FeedbackListItem
import com.reborn.core.ui.component.SectionTitleComponent

@Composable
fun FeedbackListSection(
    recentFeedbacks: List<FeedbackListItem>,
    onFeedbackClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitleComponent(title = "실시간 피드백", onMoreClick = onMoreClick)
        FeedbackList(
            items = recentFeedbacks,
            onItemClick = onFeedbackClick
        )
    }
}