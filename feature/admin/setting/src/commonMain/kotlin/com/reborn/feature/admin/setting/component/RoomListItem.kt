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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.component.TutorialHighlightOverlay
import com.reborn.core.ui.component.TutorialHintCard
import com.reborn.core.ui.component.tutorialTarget
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
    // 방장(#추가 API) 여부 - 방장만 장소 하드 삭제/방장 위임 가능, 그 외 관리자는 나가기만 가능
    isOwner: Boolean = false,
    admins: List<AdminSettingUiState.AdminProfile>,
    onDeleteClick: () -> Unit,
    onLeaveClick: () -> Unit = {},
    onTransferOwnerClick: (Long) -> Unit = {},
    onAddAdminClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onAddArduinoClick: () -> Unit,
    onAddAiSpeakerClick: () -> Unit,
    onFeedbackQrClick: () -> Unit = {},
    showAddDeviceHint: Boolean = false,
    onDismissAddDeviceHint: () -> Unit = {}
){
    var showAddSheet by remember { mutableStateOf(false) }
    var showAdminsSheet by remember { mutableStateOf(false) }
    var showTransferOwnerSheet by remember { mutableStateOf(false) }

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
            isOwner = isOwner,
            otherAdmins = admins.filterNot { it.isOwner },
            onDismiss = { showAddSheet = false },
            onAddAdminClick = onAddAdminClick,
            onAddArduinoClick = onAddArduinoClick,
            onAddAiSpeakerClick = onAddAiSpeakerClick,
            onAddDeviceClick = onAddDeviceClick,
            onFeedbackQrClick = onFeedbackQrClick,
            onDeleteClick = onDeleteClick,
            onLeaveClick = onLeaveClick,
            onOpenTransferOwnerSheet = { showTransferOwnerSheet = true },
            showAddDeviceHint = showAddDeviceHint,
            onDismissAddDeviceHint = onDismissAddDeviceHint
        )
    }

    if (showAdminsSheet) {
        AdminsBottomSheet(
            admins = admins,
            onDismiss = { showAdminsSheet = false }
        )
    }

    if (showTransferOwnerSheet) {
        TransferOwnerBottomSheet(
            admins = admins.filterNot { it.isOwner },
            onSelect = { userId ->
                showTransferOwnerSheet = false
                onTransferOwnerClick(userId)
            },
            onDismiss = { showTransferOwnerSheet = false }
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
    isOwner: Boolean,
    otherAdmins: List<AdminSettingUiState.AdminProfile>,
    onDismiss: () -> Unit,
    onAddAdminClick: () -> Unit,
    onAddArduinoClick: () -> Unit,
    onAddAiSpeakerClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onFeedbackQrClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onOpenTransferOwnerSheet: () -> Unit,
    showAddDeviceHint: Boolean = false,
    onDismissAddDeviceHint: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    // 최초 접속 튜토리얼(#240) - 바텀시트는 별도 Popup 레이어라 하이라이트 오버레이/설명 카드를
    // App.kt 바텀 네비 대신 이 시트 안에서 자체적으로 그린다.
    var addDeviceHintRect by remember { mutableStateOf<Rect?>(null) }

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
        Box {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // 최초 접속 튜토리얼(#240) - "장소 삭제/나가기" 등 항목이 시트 하단에 고정돼
                // 있어서 설명 카드를 목록 중간에 끼워 넣으면 겹쳐 보일 수 있다 - 항상 맨 위에 둔다.
                if (showAddDeviceHint) {
                    TutorialHintCard(
                        text = "여기서 아두이노나 AI 스피커를 등록할 수 있어요.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                AddSheetItem(text = "관리자 초대", onClick = { selectAndDismiss(onAddAdminClick) })
                Column(modifier = Modifier.tutorialTarget { addDeviceHintRect = it }) {
                    AddSheetItem(text = "아두이노 추가", onClick = { selectAndDismiss(onAddArduinoClick) })
                    AddSheetItem(text = "AI 스피커 추가", onClick = { selectAndDismiss(onAddAiSpeakerClick) })
                }
                AddSheetItem(text = "공기계 추가", onClick = { selectAndDismiss(onAddDeviceClick) })
                AddSheetItem(text = "피드백 QR", onClick = { selectAndDismiss(onFeedbackQrClick) })
                if (isOwner && otherAdmins.isNotEmpty()) {
                    AddSheetItem(
                        text = "방장 위임",
                        onClick = { selectAndDismiss(onOpenTransferOwnerSheet) }
                    )
                }
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                if (isOwner) {
                    AddSheetItem(
                        text = "장소 삭제",
                        textColor = RebornTheme.color.reject,
                        onClick = { showDeleteConfirm = true }
                    )
                } else {
                    AddSheetItem(
                        text = "장소 나가기",
                        textColor = RebornTheme.color.reject,
                        onClick = { showLeaveConfirm = true }
                    )
                }
            }

            if (showAddDeviceHint) {
                TutorialHighlightOverlay(
                    highlightRect = addDeviceHintRect,
                    onDismiss = onDismissAddDeviceHint
                )
            }
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

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = {
                Text(
                    "장소를 나갈까요?",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
            },
            text = {
                Text(
                    "나가면 이 장소의 목록에서 더 이상 보이지 않아요. 장소 자체는 그대로 유지돼요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700
                )
            },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; selectAndDismiss(onLeaveClick) }) {
                    Text("나가기", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.reject)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("취소", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale700)
                }
            }
        )
    }
}

// 방장 위임 대상 선택 바텀시트 - AddSheet가 닫힌 뒤(RoomListItem 레벨) 별도로 뜬다. AddSheet
// 내부에 두면 selectAndDismiss로 AddSheet 자체가 dispose될 때 상태가 함께 사라져버린다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferOwnerBottomSheet(
    admins: List<AdminSettingUiState.AdminProfile>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<AdminSettingUiState.AdminProfile?>(null) }

    // AddSheet의 selectAndDismiss와 동일한 패턴 - hide 애니메이션이 끝난 뒤에 콜백을 실행해서
    // 시트가 애니메이션 없이 뚝 사라지지 않게 한다.
    fun confirmAndDismiss(userId: Long) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onSelect(userId)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RebornTheme.color.grayScale100
    ) {
        Text(
            "방장 위임",
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Text(
            "방장을 넘겨줄 관리자를 선택하세요.",
            style = RebornTheme.typography.bodyMedium,
            color = RebornTheme.color.grayScale700,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp, bottom = 36.dp)
        ) {
            items(items = admins, key = { it.userId }) { admin ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { target = admin }
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

    target?.let { candidate ->
        AlertDialog(
            onDismissRequest = { target = null },
            title = {
                Text(
                    "${candidate.name}님에게 방장을 위임할까요?",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
            },
            text = {
                Text(
                    "위임하면 이후 장소 삭제·방장 위임 권한이 ${candidate.name}님에게 넘어가요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmAndDismiss(candidate.userId) }) {
                    Text("위임", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale900)
                }
            },
            dismissButton = {
                TextButton(onClick = { target = null }) {
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
