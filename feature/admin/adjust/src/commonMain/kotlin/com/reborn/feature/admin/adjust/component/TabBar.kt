package com.reborn.feature.admin.adjust.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme


@Composable
fun <T> TabBar(
    modifier: Modifier = Modifier,
    tabItems: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    getDisplayName: (T) -> String // 각 탭의 이름을 어떻게 가져올지 정의
) {
    val baseLineColor = RebornTheme.color.grayScale500
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .drawBehind {
                drawLine(
                    color = baseLineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ){
        tabItems.forEach { tab ->
            TabItem(
                modifier = Modifier.weight(1f),
                name = getDisplayName(tab),
                onClick = { onTabSelected(tab) },
                isSelected = tab == selectedTab
            )
        }
    }
}


@Composable
fun TabItem(
    modifier: Modifier = Modifier,
    name: String,
    onClick: () -> Unit,
    isSelected: Boolean
){
    val indicatorColor = RebornTheme.color.grayScale800
    val textColor = if (isSelected) RebornTheme.color.grayScale800 else RebornTheme.color.grayScale500
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    drawLine(
                        color = indicatorColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = name,
            style = RebornTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}