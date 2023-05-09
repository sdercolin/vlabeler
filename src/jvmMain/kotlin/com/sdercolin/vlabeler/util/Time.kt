package com.sdercolin.vlabeler.util

fun toFrame(millis: Float, sampleRate: Float) = millis * sampleRate / 1000
fun toMillisecond(frame: Float, sampleRate: Float) = 1000 * frame / sampleRate

/**
 * Get the text to display for a given time in milliseconds.
 */
fun getTimeText(millis: Double): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val base = when {
        hours >= 1 -> String.format("%d:%02d:%02d", hours.toInt(), minutes.toInt() % 60, seconds.toInt() % 60)
        minutes >= 1 -> String.format("%d:%02d", minutes.toInt(), seconds.toInt() % 60)
        else -> String.format("%d", seconds.toInt())
    }
    val remainder = millis % 1000
    if (remainder == 0.0) return base
    val millisStr = (remainder / 1000).toStringTrimmed().substringAfter(".")
    return "$base.$millisStr"
}
