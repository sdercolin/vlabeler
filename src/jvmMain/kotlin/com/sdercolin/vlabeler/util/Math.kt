package com.sdercolin.vlabeler.util

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.roundToDecimalDigit(n: Int): Float = 10.0.pow(n).let { times(it).roundToInt().toFloat().div(it.toFloat()) }
fun Double.roundToDecimalDigit(n: Int): Double = 10.0.pow(n).let { times(it).roundToInt().toFloat().div(it) }
