package com.reborn.feature.admin.setting.component

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.admin.setting.Res
import com.reborn.feature.admin.setting.ic_admin
import com.reborn.feature.admin.setting.ic_device
import com.reborn.feature.admin.setting.ic_more_vert
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RoomListItem(
    placeId: Int,
    roomName: String,
    adminCount: Int,
    deviceCount: Int?,
    onDeleteClick: () -> Unit,
    onAddAdminClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onAddArduinoClick: () -> Unit,
    onAddAiSpeakerClick: () -> Unit
){
    var showAddSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RebornTheme.color.grayScale100)
            .border(
                width = 1.dp,
                color = RebornTheme.color.grayScale200,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ){
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            Row {
                Text(
                    roomName,
                    style = RebornTheme.typography.headlineMedium,
                    color = RebornTheme.color.grayScale900
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painterResource(Res.drawable.ic_more_vert),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showAddSheet = true },
                    contentDescription = "더 보기",
                    tint = RebornTheme.color.grayScale900
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ){
                RoomInformChip(type = RoomInformType.Admin, value = adminCount)
                RoomInformChip(type = RoomInformType.IoT, value = deviceCount)
            }
        }
    }

    if (showAddSheet) {
        AddSheet(
            onDismiss = { showAddSheet = false },
            onAddAdminClick = onAddAdminClick,
            onAddArduinoClick = onAddArduinoClick,
            onAddAiSpeakerClick = onAddAiSpeakerClick,
            onAddDeviceClick = onAddDeviceClick,
            onDeleteClick = onDeleteClick
        )
    }
}

// 장소 카드 우측 상단 점3개(⋮) 클릭 시 뜨는 바텀시트 - Figma에는 별도 프레임 없이 사용자가 구두로
// 지정한 스펙(관리자/아두이노/AI스피커/공기계 4개 추가 항목) 그대로 구현. 장소 삭제는 원래 별도
// 아이콘으로 상시 노출돼 있었는데(#155), 카드 헤더가 아이콘 2개로 붐비고 오조작 위험도 있어 이 시트
// 안으로 통합(#177) - 구분선 아래 파괴적 동작으로 시각적으로 분리, 확인 없이 바로 실행되지 않도록
// 별도 확인 다이얼로그를 한 번 더 거치게 함
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSheet(
    onDismiss: () -> Unit,
    onAddAdminClick: () -> Unit,
    onAddArduinoClick: () -> Unit,
    onAddAiSpeakerClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun selectAndDismiss(onClick: () -> Unit) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            // hide 애니메이션이 취소/중단돼 시트가 여전히 보이는 상태면(예: 드래그로 다시 올림)
            // 콜백을 실행하지 않는다 - CodeRabbit 리뷰(#155)
            if (!sheetState.isVisible) {
                onDismiss()
                onClick()
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
            AddSheetItem(text = "관리자 초대", onClick = { selectAndDismiss(onAddAdminClick) })
            AddSheetItem(text = "아두이노 추가", onClick = { selectAndDismiss(onAddArduinoClick) })
            AddSheetItem(text = "AI 스피커 추가", onClick = { selectAndDismiss(onAddAiSpeakerClick) })
            AddSheetItem(text = "공기계 추가", onClick = { selectAndDismiss(onAddDeviceClick) })
            HorizontalDivider(color = RebornTheme.color.grayScale300)
            AddSheetItem(
                text = "장소 삭제",
                textColor = RebornTheme.color.reject,
                onClick = { showDeleteConfirm = true }
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("장소를 삭제할까요?", style = RebornTheme.typography.titleMedium, color = RebornTheme.color.grayScale900) },
            text = {
                Text(
                    "삭제하면 이 장소에 연결된 모든 기기 정보도 함께 사라져요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; selectAndDismiss(onDeleteClick) }) {
                    Text("삭제", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.reject)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale700)
                }
            }
        )
    }
}

@Composable
private fun AddSheetItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = RebornTheme.color.grayScale900
) {
    Text(
        text = text,
        style = RebornTheme.typography.titleSmall,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
private fun RoomInformChip(
    modifier: Modifier = Modifier,
    type: RoomInformType,
    value: Int?,
) {
    val style = getUiStyleForType(type)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(RebornTheme.color.grayScale300)
            .padding(12.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        Icon(
            painterResource(style),
            modifier = Modifier.size(16.dp),
            contentDescription = null,
            tint = RebornTheme.color.grayScale900
        )
        Text(
            // 조회 실패(null)는 "-"로 표시 - 0대와 혼동되지 않도록
            value?.toString() ?: "-",
            style = RebornTheme.typography.labelLarge,
            color = RebornTheme.color.grayScale900
        )
    }
}

enum class RoomInformType {
    Admin, IoT
}

@Composable
private fun getUiStyleForType(type: RoomInformType): DrawableResource {
    return when (type) {
        RoomInformType.Admin -> Res.drawable.ic_admin
        RoomInformType.IoT -> Res.drawable.ic_device
    }
}