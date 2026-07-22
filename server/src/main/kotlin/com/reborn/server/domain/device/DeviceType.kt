package com.reborn.server.domain.device

enum class DeviceType {
    ARDUINO,
    AEROMETER,

    // SmartThings에 등록된 기기(에어컨 등)를 이 장소의 제어/메트릭 수집 대상으로 등록할 때 사용.
    // deviceKey에는 SmartThings deviceId를 저장하며, appToken은 사용하지 않는다(#130).
    SMART_THINGS,

    // ATOM ECHO 기반 음성 피드백 기기(#142). 등록 방식은 ARDUINO와 동일(수동 등록, deviceKey 사전 등록 필요).
    AI_SPEAKER,

    ;

    companion object {
        // 판매용 기기 사전 발급 시리얼(#147)의 앞 2자리 타입 프리픽스. 재고 대상은 실물로 판매하는
        // ARDUINO/AI_SPEAKER뿐 — AEROMETER(페어링 코드)와 SMART_THINGS(SmartThings deviceId)는
        // 다른 등록 방식을 그대로 쓴다.
        val SERIAL_PREFIXES: Map<String, DeviceType> = mapOf(
            "AR" to ARDUINO,
            "AI" to AI_SPEAKER,
        )
    }

    val serialPrefix: String?
        get() = SERIAL_PREFIXES.entries.find { it.value == this }?.key
}
