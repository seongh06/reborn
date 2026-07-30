package com.reborn.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// SmartThings OAuth 동의를 마친 후 서버 콜백 페이지가 커스텀 스킴 딥링크(reborn://smartthings/callback)로
// 앱을 다시 열면, 플랫폼별 진입점(Android MainActivity)이 여기로 결과를 흘려보낸다. 앱이 이미
// AdminSmartThingsAddScreen에 떠 있는 채로 백그라운드에 있다가 딥링크로 복귀하는 경우를 위한 것이라,
// 네비게이션 파라미터로 전달하기보다 가벼운 이벤트 버스가 더 단순하다(#239) - 이 화면의 ViewModel이
// 살아있는 동안만 수신하면 되고, 콜드 스타트(프로세스가 완전히 죽은 채 대기)는 스코프 밖.
object SmartThingsCallbackSignal {
    private val _events = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(success: Boolean) {
        _events.tryEmit(success)
    }
}
