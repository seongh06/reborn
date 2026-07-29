package com.reborn.feature.admin.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.admin.setting.Res
import com.reborn.feature.admin.setting.ic_more_vert
import com.reborn.feature.admin.setting.ic_person
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

// Figma(node 595:5359 "Frame 380/place 설정") 기준 재디자인(#217) - 장소명 + 관리자 프로필만
// 보여주고, IoT 기기 개수 칩은 없앰(사용자 요청). 관리자 아바타를 겹쳐서 최대 N명까지 보여주고
// 나머지는 "+N명"으로, 탭하면 전체 관리자 목록(프로필+이름) 바텀시트가 뜬다.
@Composable
fun RoomListItem(
    placeId: Int,
    roomName: String,
    admins: List<AdminSettingUiState.AdminProfile>,
    onDeleteClick: () -> Unit,
    onAddAdminClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onAddArduinoClick: () -> Unit,
    onAddAiSpeakerClick: () -> Unit
){
    var showAddSheet by remember { mutableStateOf(false) }
    var showAdminsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RebornTheme.color.grayScale100)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ){
        Row {
            Text(
                roomName,
                style = RebornTheme.typography.headlineMedium,
                color = RebornTheme.color.grayScale900
            )
            Spacer(modifier = Modifier.weight(1f))
            // 디자인 상 세로 점3개 아이콘이나, 이 프로젝트 전반에서 "더 보기" 액션에 이미 쓰이고
            // 있는 ic_more_vert를 그대로 재사용(새 아이콘 에셋을 따로 추가하지 않음).
            Icon(
                painterResource(Res.drawable.ic_more_vert),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showAddSheet = true },
                contentDescription = "더 보기",
                tint = RebornTheme.color.grayScale900
            )
        }
        if (admins.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showAdminsSheet = true }
            ) {
                AdminAvatarStack(admins = admins)
                val overflow = admins.size - AVATAR_STACK_VISIBLE_COUNT
                if (overflow > 0) {
                    Text(
                        text = "+ ${overflow}명",
                        style = RebornTheme.typography.labelMedium,
                        color = RebornTheme.color.grayScale900,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
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

    if (showAdminsSheet) {
        AdminsBottomSheet(
            admins = admins,
            onDismiss = { showAdminsSheet = false }
        )
    }
}

private const val AVATAR_STACK_VISIBLE_COUNT = 3
private val AVATAR_SIZE = 32.dp
private val AVATAR_OVERLAP = 16.dp

@Composable
private fun AdminAvatarStack(admins: List<AdminSettingUiState.AdminProfile>) {
    val visible = admins.take(AVATAR_STACK_VISIBLE_COUNT)
    Row(horizontalArrangement = Arrangement.spacedBy(-AVATAR_OVERLAP)) {
        visible.forEach { admin ->
            AdminAvatar(
                profileImage = admin.profileImage,
                modifier = Modifier.border(2.dp, RebornTheme.color.grayScale100, CircleShape)
            )
        }
    }
}

@Composable
private fun AdminAvatar(profileImage: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(RebornTheme.color.grayScale300),
        contentAlignment = Alignment.Center
    ) {
        if (profileImage != null) {
            AsyncImage(
                model = profileImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(Res.drawable.ic_person),
                modifier = Modifier.size(AVATAR_SIZE).clip(CircleShape)
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_person),
                contentDescription = null,
                tint = RebornTheme.color.grayScale500,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminsBottomSheet(
    admins: List<AdminSettingUiState.AdminProfile>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RebornTheme.color.grayScale100
    ) {
        Text(
            "관리자",
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(items = admins, key = { it.userId }) { admin ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    AdminAvatar(profileImage = admin.profileImage)
                    Text(
                        admin.name,
                        style = RebornTheme.typography.titleSmall,
                        color = RebornTheme.color.grayScale900
                    )
                }
            }
        }
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
            title = {
                Text(
                    "장소를 삭제할까요?",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
            },
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
