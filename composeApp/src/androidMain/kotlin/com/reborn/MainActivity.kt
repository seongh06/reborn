package com.reborn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reborn.core.notification.EXTRA_FEEDBACK_ID

class MainActivity : ComponentActivity() {
    private var pendingFeedbackId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingFeedbackId = intent.extractFeedbackId()
        setContent { App(initialFeedbackId = pendingFeedbackId) }
    }

    // launchMode=singleTask라 앱이 이미 떠 있는 상태로 알림을 탭하면 onCreate가 아니라
    // 여기로 새 인텐트가 전달된다 - pendingFeedbackId를 갱신하면 setContent의 App()이 재구성된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingFeedbackId = intent.extractFeedbackId()
    }

    private fun Intent.extractFeedbackId(): Int? =
        getStringExtra(EXTRA_FEEDBACK_ID)?.toIntOrNull()
}
