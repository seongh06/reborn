package com.reborn.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reborn.core.domain.usecase.UpdateFcmTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "reborn_default_channel"
private const val CHANNEL_NAME = "Reborn 알림"
private const val TAG = "RebornMessaging"

// MainActivity가 이 extra 키로 콜드/웜 스타트 양쪽에서 읽어 피드백 상세로 딥링크한다.
const val EXTRA_FEEDBACK_ID = "feedbackId"

class RebornMessagingService : FirebaseMessagingService() {

    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            updateFcmTokenUseCase(token)
                .onFailure { android.util.Log.e(TAG, "FCM 토큰 서버 갱신 실패: ${it.message}", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        showNotification(title, body, feedbackId = message.data["feedbackId"])
    }

    private fun showNotification(title: String, body: String, feedbackId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            )
        }

        // core:notification은 composeApp의 MainActivity를 컴파일 타임에 참조할 수 없어(의존성 역방향),
        // 런처 액티비티를 패키지매니저로 조회해 인텐트를 구성한다. MainActivity는 singleTask라 앱이
        // 이미 떠 있으면 onNewIntent로, 콜드 스타트면 onCreate의 intent extras로 이 값을 받는다.
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                feedbackId?.let { putExtra(EXTRA_FEEDBACK_ID, it) }
            }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                feedbackId?.hashCode() ?: 0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: 앱 아이콘 리소스로 교체
            .setAutoCancel(true)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
