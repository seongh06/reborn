package com.reborn.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reborn.core.ui.Res
import com.reborn.core.ui.ic_more
import org.jetbrains.compose.resources.painterResource

@Composable
fun SectionTitleComponent(
    title: String,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        onMoreClick?.let {
            IconButton(onClick = onMoreClick) {
                Icon(
                    painter = painterResource(Res.drawable.ic_more),
                    contentDescription = "더 보기"
                )
            }
        }
    }
}