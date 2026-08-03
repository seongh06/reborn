package com.reborn.core.common

import androidx.compose.runtime.Composable

// 공기계 모드는 센서 수집을 위해 화면을 계속 띄워둬야 하는 전용 기기라(#142), 이 컴포저블이
// 컴포지션에 있는 동안 화면 자동 꺼짐을 막는다 - 동영상 재생 앱의 화면 유지와 동일한 개념.
@Composable
expect fun KeepScreenOn()
