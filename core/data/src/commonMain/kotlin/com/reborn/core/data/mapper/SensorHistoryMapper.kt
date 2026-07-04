package com.reborn.core.data.mapper

import com.reborn.core.model.SensorPoint
import com.reborn.core.network.model.SensorHistoryResponse

// dailyData의 값 배열은 index 0 = 0시(자정)부터 1시간 단위로 정렬되어 있다고 가정 (데이터 누락 없음)
fun SensorHistoryResponse.toSensorPoints(): List<SensorPoint> {
    return dailyData.toSortedMap().flatMap { (date, hourlyValues) ->
        hourlyValues.mapIndexed { hour, value -> SensorPoint(date = date, hour = hour, value = value) }
    }
}
