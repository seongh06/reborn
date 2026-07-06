package com.reborn.server.global.fcm

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.reborn.server.global.async.AsyncConfig
import com.reborn.server.global.util.mask
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class FcmClient(
    private val firebaseApp: FirebaseApp?,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async(AsyncConfig.ASYNC_EXECUTOR)
    fun send(fcmToken: String, title: String, body: String) {
        val app = firebaseApp
        if (app == null) {
            log.warn("FCM이 설정되지 않아 알림 발송을 건너뜁니다 (token={})", fcmToken.mask())
            return
        }

        val message = Message.builder()
            .setToken(fcmToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build()

        runCatching {
            FirebaseMessaging.getInstance(app).send(message)
        }.onFailure { e ->
            log.error("FCM 발송 실패 (token={})", fcmToken.mask(), e)
        }
    }
}
