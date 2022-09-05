package com.sdercolin.vlabeler.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun parseIsoTime(time: String): Long {
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    return LocalDateTime.parse(time, formatter).toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun getLocalDate(time: Long): String {
    val zoneOffset = OffsetDateTime.now().offset
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDateTime.ofEpochSecond(time / 1000, 0, zoneOffset).format(formatter)
}
