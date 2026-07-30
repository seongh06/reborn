package com.reborn.feature.intro.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.intro.model.PlaceType

// Figma(node 595:4831 "Text and image", signup 화면) 기준 - 이미지 없이 제목+설명 텍스트만
// 있는 카드(#252 후속). placeType.image는 더 이상 이 화면에서 쓰지 않는다.
@Composable
fun PlaceTypeList(
    placeType: PlaceType,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    val backgroundColor = if (selected) RebornTheme.color.grayScale300 else RebornTheme.color.grayScale100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = placeType.label,
            style = RebornTheme.typography.titleLarge,
            color = RebornTheme.color.grayScale900
        )
        Text(
            text = placeType.description,
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale700
        )
    }
}