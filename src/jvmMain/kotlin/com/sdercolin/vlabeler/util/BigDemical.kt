package com.sdercolin.vlabeler.util

fun Float.multiplyWithBigDecimal(other: Float): Float {
    return this.toBigDecimal().multiply(other.toBigDecimal()).toFloat()
}

fun Float.divideWithBigDecimal(other: Float): Float {
    return this.toBigDecimal().divide(other.toBigDecimal()).toFloat()
}
