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
import com.reborn.core.ui.component.FeedbackType
import com.reborn.core.ui.component.SectionTitleComponent
import com.reborn.core.ui.component.State

// TODO: 서버 feedback API 연동 전까지의 목업 데이터. 실제 연동 시 UseCase로 대체 예정 - id는
// AdminFeedbackViewModel의 mock 목록과 반드시 같은 값을 가리켜야 함(#177, 클릭 시 다른 내용의 상세로
// 이동하던 버그 - 두 화면이 서로 다른 mock 데이터셋을 따로 갖고 있어서 id는 같은데 내용이 달랐음)
private val mockRecentFeedbacks = listOf(
    FeedbackListItem(3, FeedbackType.AIR, State.REJECT, "5분전", "공기가 안 좋아요"),
    FeedbackListItem(1, FeedbackType.HOT, State.WAITING, "10분전", "너무 더워요"),
    FeedbackListItem(2, FeedbackType.LIGHT, State.APPROVE, "1시간전", "불이 너무 밝아요")
)

@Composable
fun FeedbackListSection(
    onFeedbackClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitleComponent(title = "실시간 피드백", onMoreClick = onMoreClick)
        FeedbackList(
            items = mockRecentFeedbacks,
            onItemClick = onFeedbackClick
        )
    }
}