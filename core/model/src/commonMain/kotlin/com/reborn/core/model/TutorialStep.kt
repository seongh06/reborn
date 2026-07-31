package com.reborn.core.model

// 최초 접속 튜토리얼(#240) 각 단계 id - 화면마다 독립 트리거라 순서 없이 각자 자기 id로
// tutorialSeenSteps(Set<String>)에 자신을 기록한다.
object TutorialStep {
    const val HOME_SMART_THINGS = "home_smart_things"
    const val HOME_FIRST_FEEDBACK = "home_first_feedback"
    const val ADJUST_REMOTE_TAB = "adjust_remote_tab"
    const val ADJUST_AUTO_TAB = "adjust_auto_tab"
}
