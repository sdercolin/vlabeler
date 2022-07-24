package com.sdercolin.vlabeler.util

fun Float.toStringTrimmed(): String = this.toString()
    .runIf(this.toString().contains(".")) { trim('0') }
    .trim('.')
