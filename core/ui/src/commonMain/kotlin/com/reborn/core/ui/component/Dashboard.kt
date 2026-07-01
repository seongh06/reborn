package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun Dashboard(
    place: String,
    temperature: Int,
    humidity: Int,
    illuminance: Int,
    peopleCount: Int
) {
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "$place DashBoard",
            style = RebornTheme.typography.titleMedium,
            color = RebornTheme.color.grayScale900
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardItem(modifier = Modifier.weight(1f), type = DataType.Temperature, value = temperature)
            DashboardItem(modifier = Modifier.weight(1f), type = DataType.Humidity, value = humidity)
            DashboardItem(modifier = Modifier.weight(1f), type = DataType.Illuminance, value = illuminance)
            DashboardItem(modifier = Modifier.weight(1f), type = DataType.PeopleCount, value = peopleCount)
        }
    }
}

enum class DataType {
    Temperature, Humidity, Illuminance, PeopleCount
}

data class UiStyle(
    val icon: DrawableResource,
    val color: Color,
    val gradient: Brush
)

@Composable
fun getUiStyleForType(type: DataType): UiStyle {
    return when (type) {
        DataType.Temperature -> UiStyle(
            icon = Res.drawable.ic_temperature,
            color = RebornTheme.color.temperature,
            gradient = RebornTheme.color.temperatureRadial
        )
        DataType.Humidity -> UiStyle(
            icon = Res.drawable.ic_humidity,
            color = RebornTheme.color.humidity,
            gradient = RebornTheme.color.humidityRadial
        )
        DataType.Illuminance -> UiStyle(
            icon = Res.drawable.ic_illuminance,
            color = RebornTheme.color.illuminance,
            gradient = RebornTheme.color.illuminanceRadial
        )
        DataType.PeopleCount -> UiStyle(
            icon = Res.drawable.ic_people,
            color = RebornTheme.color.peopleCount,
            gradient = RebornTheme.color.peopleCountRadial
        )
    }
}

@Composable
fun DashboardItem(
    modifier: Modifier = Modifier,
    type: DataType,
    value: Int,
    goal: Int?=null
) {
    val style = getUiStyleForType(type)

    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(style.gradient)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(style.icon),
            modifier = Modifier.size(32.dp),
            contentDescription = style.toString(),
            tint = style.color
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ){
            Text(
                text = value.toString(),
                style = RebornTheme.typography.titleLarge,
                color = RebornTheme.color.grayScale100
            )
            goal?.let { goal ->
                Text(
                    text = goal.toString(),
                    style = RebornTheme.typography.caption,
                    color = RebornTheme.color.grayScale100
                )
            }
        }
    }
}
