package com.sdercolin.vlabeler.util

import androidx.compose.foundation.ScrollState

typealias FloatRange = ClosedFloatingPointRange<Float>

/**
 * Returns [ScrollState.value] range of the current displayed screen range. If the scroll state is not initialized,
 * returns null.
 */
fun ScrollState.getScreenRange(canvasLength: Float): FloatRange? {
    if (maxValue == Int.MAX_VALUE) return null
    val screenLength = canvasLength - maxValue
    if (screenLength <= 0) return null
    val start = value.toFloat()
    val end = value.toFloat() + screenLength
    return start..end
}

operator fun FloatRange.contains(other: FloatRange): Boolean {
    return !(other.endInclusive < start || other.start > endInclusive)
}

val FloatRange.length get() = endInclusive - start
