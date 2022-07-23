package com.sdercolin.vlabeler.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun String.toColor(): Color {
    val hex = replace("#", "").padStart(8, 'f').toLong(16)
    return Color(hex)
}

fun String.toRgbColor() = toColor().copy(alpha = 1f)
fun String.toColorOrNull() = runCatching { toColor() }.getOrNull()
fun String.toRgbColorOrNull() = runCatching { toRgbColor() }.getOrNull()

val Color.rgbHexString get() = String.format("#%06X", 0xffffff and toArgb())
val Color.argbHexString get() = String.format("#%08X", 0xffffffff and toArgb().toLong())

val Char.isHexChar get() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
