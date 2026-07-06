package com.reborn.feature.admin.feedback.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme

@Composable
fun FeedbackStatusSection(
    modifier: Modifier = Modifier,
    totalCount: Int,
    waitingCount: Int,
    isOnline: Boolean = false
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale100)
            .border(
                width = 1.dp,
                color = RebornTheme.color.grayScale200,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(20.dp,12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Text(
            "미확인 피드백 : $waitingCount",
            style = RebornTheme.typography.titleSmall,
            color = RebornTheme.color.grayScale900
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                "전체 : $totalCount",
                style = RebornTheme.typography.titleSmall,
                color = RebornTheme.color.grayScale900
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) RebornTheme.color.approve else RebornTheme.color.reject)
            )
        }
    }
}