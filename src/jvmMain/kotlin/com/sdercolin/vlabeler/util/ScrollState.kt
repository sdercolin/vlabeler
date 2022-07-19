package com.sdercolin.vlabeler.util

import androidx.compose.foundation.ScrollState

typealias FloatRange = ClosedFloatingPointRange<Float>

fun ScrollState.getScreenRange(canvasLength: Int): FloatRange? {
    if (maxValue == Int.MAX_VALUE) return null
    val screenLength = canvasLength.toFloat() - maxValue
    if (screenLength <= 0) return null
    val start = value.toFloat()
    val end = value.toFloat() + screenLength
    return start..end
}

operator fun FloatRange.contains(other: FloatRange): Boolean {
    return !(other.endInclusive < start || other.start > endInclusive)
}

val FloatRange.length get() = endInclusive - start
