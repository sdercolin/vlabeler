package com.sdercolin.vlabeler.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun parseColor(colorString: String): Color {
    val hex = colorString.replace("#", "ff").toLong(16)
    return Color(hex)
}

val Color.hexString get() = String.format("#%06X", 0xFFFFFF and toArgb())
