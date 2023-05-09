package com.sdercolin.vlabeler.util

/**
 * Converts a [Float] to a [String] and trims the trailing zeros and the decimal point.
 */
fun Float.toStringTrimmed(): String = this.toDouble().toStringTrimmed()

/**
 * Converts a [Double] to a [String] and trims the trailing zeros and the decimal point.
 */
fun Double.toStringTrimmed(): String {
    var str = this.toBigDecimal().toPlainString()
    if (str.contains(".")) {
        str = str.trimEnd('0')
    }
    str = str.trim('.')
    return str
}
