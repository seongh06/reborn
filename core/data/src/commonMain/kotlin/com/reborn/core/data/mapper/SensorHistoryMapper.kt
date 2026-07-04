package com.reborn.core.data.mapper

import com.reborn.core.model.SensorPoint
import com.reborn.core.network.model.SensorHistoryResponse

// dailyData의 값 배열은 index 0 = 0시(자정)부터 1시간 단위로 정렬되어 있다고 가정 (데이터 누락 없음)
// toSortedMap()은 JVM 전용이라 KMP commonMain에서 못 쓰므로 정렬된 리스트로 변환 후 flatMap
fun SensorHistoryResponse.toSensorPoints(): List<SensorPoint> {
    return dailyData.toList()
        .sortedBy { (date, _) -> date }
        .flatMap { (date, hourlyValues) ->
            hourlyValues.mapIndexed { hour, value -> SensorPoint(date = date, hour = hour, value = value) }
        }
}
