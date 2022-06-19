package com.sdercolin.vlabeler.util

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.roundToDecimalDigit(n: Int) = 10.0.pow(n).let { times(it).roundToInt().toFloat().div(it) }
fun Double.roundToDecimalDigit(n: Int) = 10.0.pow(n).let { times(it).roundToInt().toFloat().div(it) }
