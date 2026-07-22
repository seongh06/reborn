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

private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

fun generateRandomCode(length: Int): String =
    (1..length).map { CODE_CHARS.random() }.joinToString("")

// 판매용 기기 시리얼(#147)의 랜덤 구간용 charset — 사람이 스티커를 보고 직접 타이핑하므로
// 0/O, 1/I, L처럼 혼동되는 문자를 제외한다. generateRandomCode(페어링 코드용)와는 별도 유지.
private const val SERIAL_SUFFIX_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

fun generateDeviceSerial(prefix: String, suffixLength: Int = 6): String =
    prefix + (1..suffixLength).map { SERIAL_SUFFIX_CHARS.random() }.joinToString("")