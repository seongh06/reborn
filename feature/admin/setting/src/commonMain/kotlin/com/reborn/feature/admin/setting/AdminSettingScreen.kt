package com.reborn.feature.admin.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.reborn.core.common.PickedImage
import com.reborn.core.common.rememberImagePicker
import com.reborn.core.designsystem.component.RebornButton
import com.reborn.core.designsystem.component.RebornTextField
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.SettingItem
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.setting.Res
import com.reborn.feature.admin.setting.component.RoomListItem
import com.reborn.feature.admin.setting.model.AdminSettingIntent
import com.reborn.feature.admin.setting.model.AdminSettingUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminSettingRoute(
    viewModel: AdminSettingViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToInviteCode: (Int) -> Unit = {},
    onNavigateToAddDevice: (Int) -> Unit = {},
    onNavigateToAddArduino: (Int) -> Unit = {},
    onNavigateToAddAiSpeaker: (Int) -> Unit = {},
    onNavigateToAddPlace: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminSettingIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminSettingEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminSettingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is AdminSettingEvent.Exit -> onBackClick()
                is AdminSettingEvent.NavigateToInviteCode -> onNavigateToInviteCode(event.placeId)
                is AdminSettingEvent.NavigateToAddDevice -> onNavigateToAddDevice(event.placeId)
                is AdminSettingEvent.NavigateToAddArduino -> onNavigateToAddArduino(event.placeId)
                is AdminSettingEvent.NavigateToAddAiSpeaker -> onNavigateToAddAiSpeaker(event.placeId)
                is AdminSettingEvent.NavigateToAddPlace -> onNavigateToAddPlace()
                is AdminSettingEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when (val state = uiState) {
            is AdminSettingUiState.Loading -> RebornLoadingScreen()
            is AdminSettingUiState.Setting -> AdminSettingScreen(
                state = state,
                onBackClick = onBackClick,
                onDeleteRoomClick = { placeId -> viewModel.onIntent(AdminSettingIntent.DeleteRoom(placeId)) },
                onAddAdminClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddAdmin(placeId)) },
                onAddDeviceClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddDevice(placeId)) },
                onAddArduinoClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddArduino(placeId)) },
                onAddAiSpeakerClick = { placeId -> viewModel.onIntent(AdminSettingIntent.ClickAddAiSpeaker(placeId)) },
                onAddPlaceClick = { viewModel.onIntent(AdminSettingIntent.ClickAddPlace) },
                onLogoutClick = { viewModel.onIntent(AdminSettingIntent.ClickLogout) },
                onWithdrawClick = { viewModel.onIntent(AdminSettingIntent.ClickWithdraw) },
                onProfileNameChange = { name -> viewModel.onIntent(AdminSettingIntent.UpdateProfileName(name)) },
                onProfileImagePicked = { image ->
                    viewModel.onIntent(
                        AdminSettingIntent.UpdateProfileImage(image.bytes, image.fileName, image.mimeType)
                    )
                },
                onProfileImagePickError = { throwable ->
                    scope.launch { snackbarHostState.showSnackbar(throwable.message ?: "이미지를 선택할 수 없습니다.") }
                },
                onTermsClick = onNavigateToTerms
            )
        }
    }
}

@Composable
fun AdminSettingScreen(
    state: AdminSettingUiState.Setting,
    onBackClick: () -> Unit,
    onDeleteRoomClick: (Int) -> Unit,
    onAddAdminClick: (Int) -> Unit,
    onAddDeviceClick: (Int) -> Unit,
    onAddArduinoClick: (Int) -> Unit,
    onAddAiSpeakerClick: (Int) -> Unit,
    onAddPlaceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit = {},
    onProfileNameChange: (String) -> Unit = {},
    onProfileImagePicked: (PickedImage) -> Unit = {},
    onProfileImagePickError: (Throwable) -> Unit = {},
    onTermsClick: () -> Unit = {}

) {
    var showWithdrawConfirm by remember { mutableStateOf(false) }

    if (showWithdrawConfirm) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirm = false },
            title = {
                Text(
                    "탈퇴할까요?",
                    style = RebornTheme.typography.titleMedium,
                    color = RebornTheme.color.grayScale900
                )
            },
            text = {
                Text(
                    "탈퇴하면 계정 정보가 삭제되고 되돌릴 수 없어요. 유일한 관리자로 등록된 장소가 있으면 탈퇴가 제한돼요.",
                    style = RebornTheme.typography.bodyMedium,
                    color = RebornTheme.color.grayScale700
                )
            },
            confirmButton = {
                TextButton(onClick = { showWithdrawConfirm = false; onWithdrawClick() }) {
                    Text("탈퇴", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.reject)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirm = false }) {
                    Text("취소", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .rebornDefault(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        RebornTopAppBar(title = "설정", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Profile",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            ProfileSection(
                name = state.profileName,
                imageUrl = state.profileImageUrl,
                placeTags = state.rooms.map { it.roomName },
                onNameChange = onProfileNameChange,
                onImagePicked = onProfileImagePicked,
                onImagePickError = onProfileImagePickError
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Place",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            state.rooms.forEach { room ->
                RoomListItem(
                    placeId = room.placeId,
                    roomName = room.roomName,
                    adminCount = room.adminCount,
                    deviceCount = room.deviceCount,
                    onDeleteClick = { onDeleteRoomClick(room.placeId) },
                    onAddAdminClick = { onAddAdminClick(room.placeId) },
                    onAddDeviceClick = { onAddDeviceClick(room.placeId) },
                    onAddArduinoClick = { onAddArduinoClick(room.placeId) },
                    onAddAiSpeakerClick = { onAddAiSpeakerClick(room.placeId) }
                )
            }
            RebornButton(
                modifier = Modifier.fillMaxWidth(),
                text = "새로운 place 추가",
                onClick = onAddPlaceClick
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "정보 및 기타",
                style = RebornTheme.typography.titleMedium,
                color = RebornTheme.color.grayScale900
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(RebornTheme.color.grayScale100)
                    .border(
                        width = 1.dp,
                        color = RebornTheme.color.grayScale200,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "서비스 소개", onClick = {})
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "이용약관", onClick = onTermsClick)
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "로그아웃", onClick = onLogoutClick)
                HorizontalDivider(color = RebornTheme.color.grayScale300)
                SettingItem(label = "탈퇴", onClick = { showWithdrawConfirm = true })
            }
        }
    }
}

// Figma 595:5356 기준 - 아바타(+편집 뱃지)/이름/소속 place 태그.
@Composable
private fun ProfileSection(
    name: String?,
    imageUrl: String?,
    placeTags: List<String>,
    onNameChange: (String) -> Unit = {},
    onImagePicked: (PickedImage) -> Unit = {},
    onImagePickError: (Throwable) -> Unit = {}
) {
    var isEditingName by remember { mutableStateOf(false) }
    val launchImagePicker = rememberImagePicker(
        onImagePicked = onImagePicked,
        onError = onImagePickError
    )

    if (isEditingName) {
        EditNameDialog(
            initialName = name.orEmpty(),
            onConfirm = { newName ->
                onNameChange(newName)
                isEditingName = false
            },
            onDismiss = { isEditingName = false }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RebornTheme.color.grayScale300),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        error = painterResource(Res.drawable.ic_person),
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_person),
                        contentDescription = null,
                        tint = RebornTheme.color.grayScale500,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { launchImagePicker() }
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = "프로필 이미지 변경",
                    tint = RebornTheme.color.grayScale900,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isEditingName = true }
            ) {
                Text(
                    text = name ?: "-",
                    style = RebornTheme.typography.headlineMedium,
                    color = RebornTheme.color.grayScale900
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = "이름 수정",
                    tint = RebornTheme.color.grayScale500,
                    modifier = Modifier.size(16.dp)
                )
            }
            val visibleTags = placeTags.take(2)
            val overflowCount = placeTags.size - visibleTags.size
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RebornTheme.color.grayScale300)
                            .padding(8.dp, 4.dp)
                    ) {
                        Text(
                            text = tag,
                            style = RebornTheme.typography.labelMedium,
                            color = RebornTheme.color.grayScale800
                        )
                    }
                }
                if (overflowCount > 0) {
                    Text(
                        text = "+$overflowCount",
                        style = RebornTheme.typography.labelMedium,
                        color = RebornTheme.color.grayScale800
                    )
                }
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이름 수정", style = RebornTheme.typography.titleMedium, color = RebornTheme.color.grayScale900) },
        text = {
            RebornTextField(
                value = name,
                onValueChange = { name = it },
                hint = "이름 입력",
                maxLength = 30
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("확인", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale900)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", style = RebornTheme.typography.labelLarge, color = RebornTheme.color.grayScale700)
            }
        }
    )
}
