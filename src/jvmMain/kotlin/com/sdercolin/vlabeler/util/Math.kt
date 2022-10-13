package com.sdercolin.vlabeler.util

import kotlin.math.pow
import kotlin.math.roundToLong

fun Float.roundToDecimalDigit(n: Int?): Float = if (n == null) this else {
    10.0.pow(n).let { times(it).roundToLong().toFloat().div(it.toFloat()) }
}

fun Double.roundToDecimalDigit(n: Int?): Double = if (n == null) this else {
    10.0.pow(n).let { times(it).roundToLong().toFloat().div(it) }
}
