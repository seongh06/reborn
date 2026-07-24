package com.reborn.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.Res
import com.reborn.core.ui.ic_humidity
import com.reborn.core.ui.ic_illuminance
import com.reborn.core.ui.ic_people
import com.reborn.core.ui.ic_temperature
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun Dashboard(
    temperature: Float?=null,
    humidity: Float?=null,
    illuminance: Float?=null,
    peopleCount: Float?=null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        temperature?.let{ DashboardItem(type = DataType.Temperature, value = temperature) }
        humidity?.let{ DashboardItem(type = DataType.Humidity, value = humidity) }
        illuminance?.let{ DashboardItem(type = DataType.Illuminance, value = illuminance) }
        peopleCount?.let{ DashboardItem(type = DataType.PeopleCount, value = peopleCount) }
    }
}

enum class DataType {
    Temperature, Humidity, Illuminance, PeopleCount
}

data class UiStyle(
    val icon: DrawableResource,
    val color: Color,
    val sign: String
)

@Composable
fun getUiStyleForType(type: DataType): UiStyle {
    return when (type) {
        DataType.Temperature -> UiStyle(
            icon = Res.drawable.ic_temperature,
            color = RebornTheme.color.temperature,
            sign = "°C"
        )
        DataType.Humidity -> UiStyle(
            icon = Res.drawable.ic_humidity,
            color = RebornTheme.color.humidity,
            sign = "%"
        )
        DataType.Illuminance -> UiStyle(
            icon = Res.drawable.ic_illuminance,
            color = RebornTheme.color.illuminance,
            sign = "lx"
        )
        DataType.PeopleCount -> UiStyle(
            icon = Res.drawable.ic_people,
            color = RebornTheme.color.peopleCount,
            sign = "명"
        )
    }
}

@Composable
fun DashboardItem(
    modifier: Modifier = Modifier,
    type: DataType,
    value: Float
) {
    val style = getUiStyleForType(type)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RebornTheme.color.grayScale300)
            .padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(style.icon),
            modifier = Modifier.size(24.dp),
            contentDescription = null,
            tint = style.color
        )
        Text(
            text = "${formatDashboardValue(type, value)}${style.sign}",
            style = RebornTheme.typography.bodyLarge,
            color = RebornTheme.color.grayScale700
        )
    }
}

// 온도·습도는 소수점 한 자리까지, 조도·인원수는 정수로 표시
private fun formatDashboardValue(type: DataType, value: Float): String {
    return when (type) {
        DataType.Temperature, DataType.Humidity -> {
            val sign = if (value < 0) "-" else ""
            val scaled = (abs(value) * 10).roundToInt()
            "$sign${scaled / 10}.${scaled % 10}"
        }
        DataType.Illuminance, DataType.PeopleCount -> value.roundToInt().toString()
    }
}
/*
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
            contentDescription = null,
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
*/
