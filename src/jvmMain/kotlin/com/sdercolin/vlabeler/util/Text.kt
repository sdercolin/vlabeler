package com.sdercolin.vlabeler.util

/**
 * Converts a [Float] to a [String] and trims the trailing zeros and the decimal point.
 */
fun Float.toStringTrimmed(): String = this.toString()
    .runIf(this.toString().contains(".")) { trimEnd('0') }
    .trim('.')
