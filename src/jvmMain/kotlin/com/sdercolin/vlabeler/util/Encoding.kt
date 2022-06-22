package com.sdercolin.vlabeler.util

import java.nio.charset.Charset

fun String.fromLocalEncoding(): String {
    encodeToByteArray()
    val local = Charset.defaultCharset()
    val bytes = toByteArray(local)
    return bytes.decodeToString()
}