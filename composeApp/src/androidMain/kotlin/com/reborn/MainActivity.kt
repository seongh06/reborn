package com.reborn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reborn.core.common.SmartThingsCallbackSignal
import com.reborn.core.notification.EXTRA_FEEDBACK_ID

class MainActivity : ComponentActivity() {
    private var pendingFeedbackId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingFeedbackId = intent.extractFeedbackId()
        intent.emitSmartThingsCallbackIfPresent()
        setContent { App(initialFeedbackId = pendingFeedbackId) }
    }

    // launchMode=singleTask라 앱이 이미 떠 있는 상태로 알림을 탭하면 onCreate가 아니라
    // 여기로 새 인텐트가 전달된다 - pendingFeedbackId를 갱신하면 setContent의 App()이 재구성된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingFeedbackId = intent.extractFeedbackId()
        intent.emitSmartThingsCallbackIfPresent()
    }

    private fun Intent.extractFeedbackId(): Int? =
        getStringExtra(EXTRA_FEEDBACK_ID)?.toIntOrNull()

    // SmartThings OAuth 콜백 페이지가 reborn://smartthings/callback?success=true 로 앱을
    // 다시 열면(#239), AdminSmartThingsAddViewModel이 이미 화면에 떠 있는 채로 이 신호를 받아
    // 자동으로 연동된 기기 목록을 다시 불러온다.
    private fun Intent.emitSmartThingsCallbackIfPresent() {
        val uri = data ?: return
        if (uri.scheme == "reborn" && uri.host == "smartthings") {
            SmartThingsCallbackSignal.emit(uri.getBooleanQueryParameter("success", false))
        }
    }
}
