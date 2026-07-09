package com.reborn.core.notification

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun getFcmToken(): String? = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> if (continuation.isActive) continuation.resume(token) }
        .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
}
