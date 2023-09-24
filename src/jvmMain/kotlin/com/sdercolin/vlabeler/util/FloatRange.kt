package com.sdercolin.vlabeler.util

typealias FloatRange = ClosedFloatingPointRange<Float>

operator fun FloatRange.contains(other: FloatRange): Boolean {
    return !(other.endInclusive < start || other.start > endInclusive)
}

val FloatRange.length get() = endInclusive - start
