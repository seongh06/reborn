package com.reborn.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import kotlinx.coroutines.launch

// Home/Data 화면에서 "지금 보고 있는 룸"을 바꾸는 공용 바텀시트(#166) - RoomListItem.kt의
// AdminsBottomSheet/AddSheet와 동일한 패턴(ModalBottomSheet + grayScale100 + hide-then-callback으로
// 더블파이어 방지). core:ui가 core:model(Place)에 의존하지 않도록 가벼운 RoomOption만 받는다.
// 흑백 디자인 정체성(컬러 accent 없음)이라 선택 상태는 굵기+명도 차이로만 구분한다.
data class RoomOption(val id: Long, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSwitcherBottomSheet(
    rooms: List<RoomOption>,
    selectedRoomId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun selectAndDismiss(roomId: Long) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
                onSelect(roomId)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RebornTheme.color.grayScale100
    ) {
        LazyColumn(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(items = rooms, key = { it.id }) { room ->
                val selected = room.id == selectedRoomId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { selectAndDismiss(room.id) })
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        room.name,
                        style = RebornTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale600
                    )
                }
            }
        }
    }
}
