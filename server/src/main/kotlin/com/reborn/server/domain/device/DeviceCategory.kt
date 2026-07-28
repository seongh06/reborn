package com.reborn.server.domain.device

// 기기 목록 아이콘 구분용 - 클라이언트 core:ui의 DeviceType과 이름을 그대로 맞춤(#166 QA).
// SmartThings API가 기기 카테고리를 안정적으로 안 내려줘서 등록 시 관리자가 직접 선택한다.
enum class DeviceCategory {
    LAMP, PLUG, TV, AIR_CONDITIONER, CURTAIN, OTHER
}
