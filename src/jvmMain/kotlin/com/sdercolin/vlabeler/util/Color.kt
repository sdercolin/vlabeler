package com.sdercolin.vlabeler.util

import androidx.compose.ui.graphics.Color

fun parseColor(colorString: String): Color {
    val hex = colorString.replace("#", "ff").toLong(16)
    return Color(hex)
}
