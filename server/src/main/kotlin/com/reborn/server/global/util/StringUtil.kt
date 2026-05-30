package com.reborn.server.global.util

import java.util.UUID

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    if (length <= maxLength) return this
    val cutAt = (maxLength - suffix.length).coerceAtLeast(0)
    return take(cutAt) + suffix
}

fun String.mask(visibleCount: Int = 4): String =
    if (length <= visibleCount) this
    else take(visibleCount) + "*".repeat(length - visibleCount)

fun generateUuid(): String = UUID.randomUUID().toString()
