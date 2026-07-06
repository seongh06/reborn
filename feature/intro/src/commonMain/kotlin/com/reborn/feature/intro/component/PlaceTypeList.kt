package com.reborn.feature.intro.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.feature.intro.Res
import com.reborn.feature.intro.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlaceTypeList(
    placeType: String,
    onClick: () -> Unit
) {
    val image = when (placeType) {
        "HOME" -> Res.drawable.img_home
        "STORE" -> Res.drawable.img_store
        "COMPANY" -> Res.drawable.img_company
        else -> Res.drawable.img_home
    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = placeType,
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ){
            Text(
                text = placeType,
                style = RebornTheme.typography.headlineMedium,
                color = RebornTheme.color.grayScale900
            )
            Text(
                text = "장소 설명",
                style = RebornTheme.typography.bodyMedium,
                color = RebornTheme.color.grayScale700
            )
        }
    }
}