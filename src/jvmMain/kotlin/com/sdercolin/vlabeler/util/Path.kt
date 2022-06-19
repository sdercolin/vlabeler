package com.sdercolin.vlabeler.util

import java.io.File

val HomePath get() = File(System.getProperty("user.home"))

private val invalidCharsForFileName = arrayOf('"', '*', ':', '<', '>', '?', '\\', '|', Char(0x7F), '\u0000')

fun String.isValidFileName(): Boolean {
    return invalidCharsForFileName.none { contains(it) }
}

val String.lastPathSection get() = split("/").last()
