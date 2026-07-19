package com.reborn.server.domain.device

enum class DeviceType {
    ARDUINO,
    AEROMETER,

    // SmartThings에 등록된 기기(에어컨 등)를 이 장소의 제어/메트릭 수집 대상으로 등록할 때 사용.
    // deviceKey에는 SmartThings deviceId를 저장하며, appToken은 사용하지 않는다(#130).
    SMART_THINGS,
}
