package com.sdercolin.vlabeler.util

import java.math.BigDecimal

/**
 * Converts a [Float] to a [String] and trims the trailing zeros and the decimal point.
 */
fun Float.toStringTrimmed(): String = this.toBigDecimal().toStringTrimmed()

/**
 * Converts a [Double] to a [String] and trims the trailing zeros and the decimal point.
 */
fun Double.toStringTrimmed(): String = this.toBigDecimal().toStringTrimmed()

private fun BigDecimal.toStringTrimmed(): String {
    var str = this.toPlainString()
    if (str.contains(".")) {
        str = str.trimEnd('0')
    }
    str = str.trim('.')
    return str
}

/**
 * Removes the control characters from a [String].
 */
fun String.removeControlCharacters(): String = this.filterNot { it in '\u0000'..'\u001f' }
