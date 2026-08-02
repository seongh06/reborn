package com.reborn.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 앱에 "지금 보고 있는 장소"라는 개념이 아예 없어서 Home/Data/기기 등록 화면이 전부 항상 첫 번째
// 장소만 조회하던 문제(#166)를 고치기 위한 순수 인메모리 선택 상태 - 서버 데이터가 아니라 UI 선택
// 상태라 Repository/UseCase 계층 없이 Koin 싱글턴으로 바로 주입한다. 앱 재시작 시 리셋되는데,
// 기존에도 항상 첫 번째 장소만 봤으니 회귀는 아니다.
class CurrentPlaceState {
    private val _selectedPlaceId = MutableStateFlow<Long?>(null)
    val selectedPlaceId: StateFlow<Long?> = _selectedPlaceId.asStateFlow()

    fun select(placeId: Long) {
        _selectedPlaceId.value = placeId
    }
}
