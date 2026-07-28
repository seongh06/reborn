package com.reborn.feature.admin.adjust.model

import com.reborn.core.ui.component.DeviceType

data class AutoControlUiState(
    val discomfortThreshold: String = "default",
    val discomfortAction: String = "희망 온도 2°C 낮추기",

    val humidityHighThreshold: String = "70%",
    val humidityHighAction: String = "제습 모드 시작",
    val humidityLowThreshold: String = "30%",
    val humidityLowAction: String = "난방 모드 시작",

    val temperatureHighThreshold: String = "28°C",
    val temperatureHighAction: String = "냉방 시작",
    val temperatureLowThreshold: String = "18°C",
    val temperatureLowAction: String = "난방 시작",

    val occupancyThreshold: String = "5명",
    val occupancyAction: String = "온도 1°C 낮추기",

    val isAutoOffEnabled: Boolean = false,
    val autoOffMinutes: String = "10"
)

enum class AutoControlField(val actionOptions: List<String>) {
    Discomfort(
        listOf(
            "희망 온도 1°C 낮추기",
            "희망 온도 2°C 낮추기",
            "희망 온도 3°C 낮추기",
            "희망 온도 4°C 낮추기",
            "희망 온도 5°C 낮추기",
            "전원 끄기"
        )
    ),
    HumidityHigh(listOf("제습 모드 시작", "환기 모드 시작", "냉방 모드 시작", "전원 끄기")),
    HumidityLow(listOf("난방 모드 시작", "가습 모드 시작", "전원 끄기")),
    TemperatureHigh(listOf("냉방 시작", "제습 시작", "송풍 시작", "전원 끄기")),
    TemperatureLow(listOf("난방 시작", "가습 시작", "전원 끄기")),
    Occupancy(
        listOf(
            "온도 1°C 낮추기",
            "온도 2°C 낮추기",
            "풍량 세기 증가",
            "전원 끄기"
        )
    )
}

fun AutoControlUiState.applyAction(field: AutoControlField, action: String): AutoControlUiState =
    when (field) {
        AutoControlField.Discomfort -> copy(discomfortAction = action)
        AutoControlField.HumidityHigh -> copy(humidityHighAction = action)
        AutoControlField.HumidityLow -> copy(humidityLowAction = action)
        AutoControlField.TemperatureHigh -> copy(temperatureHighAction = action)
        AutoControlField.TemperatureLow -> copy(temperatureLowAction = action)
        AutoControlField.Occupancy -> copy(occupancyAction = action)
    }

// 에어컨 외 기기(조명/플러그/TV/커튼/기타)용 축소 자동 제어 - 실내 온도 규칙만, 액션은
// 전원 켜기/끄기(SmartThings switch capability, 모든 기기 공통)뿐이라 운전모드 문구가 필요 없다.
enum class SimpleAutoControlField {
    TemperatureHigh,
    TemperatureLow
}

val SimpleAutoControlActions = listOf("켜기", "끄기")

fun AutoControlUiState.applySimpleAction(field: SimpleAutoControlField, action: String): AutoControlUiState =
    when (field) {
        SimpleAutoControlField.TemperatureHigh -> copy(temperatureHighAction = action)
        SimpleAutoControlField.TemperatureLow -> copy(temperatureLowAction = action)
    }

// 처음 진입 시(서버에 저장된 규칙이 없을 때) 보여줄 기본 액션 문구 - 에어컨은 기존 프리셋 그대로,
// 그 외 기기는 "냉방 시작" 같은 에어컨 전용 문구 대신 켜기/끄기로 초기화한다.
fun defaultAutoControlState(deviceType: DeviceType): AutoControlUiState =
    if (deviceType == DeviceType.AIR_CONDITIONER) {
        AutoControlUiState()
    } else {
        AutoControlUiState(temperatureHighAction = "끄기", temperatureLowAction = "켜기")
    }
