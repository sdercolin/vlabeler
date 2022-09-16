package com.sdercolin.vlabeler.util

fun <T> T?.or(other: T): T = this ?: other
