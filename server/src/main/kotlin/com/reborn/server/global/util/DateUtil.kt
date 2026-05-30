package com.reborn.server.global.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtil {

    private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun LocalDateTime.toDisplay(): String = format(DISPLAY_FORMATTER)

    fun LocalDateTime.toDateOnly(): String = format(DATE_FORMATTER)

    fun LocalDate.toDisplay(): String = format(DATE_FORMATTER)

    fun LocalDateTime.toIso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
