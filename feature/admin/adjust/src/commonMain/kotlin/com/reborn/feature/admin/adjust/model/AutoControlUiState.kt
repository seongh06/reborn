package com.reborn.feature.admin.adjust.model

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
