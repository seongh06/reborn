package com.reborn.server.global.util

import java.util.UUID

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    require(maxLength >= 0) { "maxLength must be >= 0" }

    if (length <= maxLength) return this

    if (maxLength <= suffix.length) return suffix.take(maxLength)

    val cutAt = (maxLength - suffix.length).coerceAtLeast(0)
    return take(cutAt) + suffix
}

fun String.mask(visibleCount: Int = 4): String {
    require(visibleCount >= 0) { "visibleCount must be >= 0" }

    return if (length <= visibleCount) this
    else take(visibleCount) + "*".repeat(length - visibleCount)
}

fun generateUuid(): String = UUID.randomUUID().toString()