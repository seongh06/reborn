package com.reborn.server.global.util

import java.util.UUID

object StringUtil {

    fun String.truncate(maxLength: Int, suffix: String = "..."): String =
        if (length > maxLength) take(maxLength) + suffix else this

    fun String.mask(visibleCount: Int = 4): String =
        if (length <= visibleCount) this
        else take(visibleCount) + "*".repeat(length - visibleCount)

    fun generateUuid(): String = UUID.randomUUID().toString()
}
