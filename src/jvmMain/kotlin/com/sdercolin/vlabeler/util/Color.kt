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

fun Color.alpha(alpha: Float) = copy(alpha = alpha)
val Color.rgbHexString get() = String.format("#%06X", 0xffffff and toArgb())
val Color.argbHexString get() = String.format("#%08X", 0xffffffff and toArgb().toLong())

val Char.isHexChar get() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

@Suppress("UnnecessaryVariable")
fun Color.toHsv(): FloatArray {
    val min = minOf(red, green, blue)
    val max = maxOf(red, green, blue)
    val delta = max - min
    val h = if (delta == 0f) {
        0f
    } else when (max) {
        red -> (green - blue) / delta
        green -> (blue - red) / delta + 2
        blue -> (red - green) / delta + 4
        else -> 0f
    }
        .let { it % 6 }
        .let { if (it < 0) it + 6 else it }
        .let { it * 60 }
    val s = if (max == 0f) 0f else delta / max
    val v = max
    return floatArrayOf(h, s, v)
}
