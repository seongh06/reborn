package com.reborn.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

// 기기 리스트(#327) 롱클릭 시 뜨는 삭제 바텀시트 - RoomSwitcherBottomSheet와 동일한 패턴
// (ModalBottomSheet + grayScale100 + hide-then-callback으로 더블파이어 방지).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRemoveBottomSheet(
    deviceName: String,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun removeAndDismiss() {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
                onRemove()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RebornTheme.color.grayScale100
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = deviceName,
                style = RebornTheme.typography.caption,
                color = RebornTheme.color.grayScale500,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = "기기 제거",
                style = RebornTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RebornTheme.color.reject,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = ::removeAndDismiss)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
