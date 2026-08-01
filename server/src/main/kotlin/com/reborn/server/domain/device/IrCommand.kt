package com.reborn.server.domain.device

// 아두이노 IR 에어컨 제어(#288) 명령 어휘. raw capture로 학습해둔 신호만 값으로 존재할 수 있음 -
// 새 온도 프리셋을 지원하려면 리모컨에서 그 신호를 먼저 캡처하고 펌웨어 raw 배열에 추가한 뒤에만
// 여기 값을 늘려야 한다(서버가 먼저 값을 추가해도 펌웨어가 모르면 무시됨).
enum class IrCommand {
    POWER_ON,
    POWER_OFF,
    COOL_24,
}
