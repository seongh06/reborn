package com.reborn.server.global.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.util.Base64

@Configuration
class FirebaseConfig(
    @param:Value("\${fcm.service-account-key-base64:}") private val serviceAccountKeyBase64: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun firebaseApp(): FirebaseApp? {
        if (serviceAccountKeyBase64.isBlank()) {
            log.warn("FCM_SERVICE_ACCOUNT_KEY_BASE64가 설정되지 않아 FCM 발송이 비활성화됩니다.")
            return null
        }
        if (FirebaseApp.getApps().isNotEmpty()) {
            return FirebaseApp.getInstance()
        }

        val credentials = GoogleCredentials.fromStream(
            ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountKeyBase64)),
        )
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        return FirebaseApp.initializeApp(options)
    }
}
