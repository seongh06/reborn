package com.reborn.feature.admin.adjust.model

enum class OperationMode(val label: String) {
    COOL("냉방"),
    HEAT("난방"),
    DEHUMIDIFY("제습"),
    FAN("송풍")
}

enum class WindSpeed(val label: String) {
    LOW("약"),
    MEDIUM("중"),
    HIGH("강"),
    AUTO("자동")
}
