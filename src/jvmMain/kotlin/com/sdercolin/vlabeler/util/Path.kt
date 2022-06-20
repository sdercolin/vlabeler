package com.sdercolin.vlabeler.util

import java.io.File

val HomePath get() = File(System.getProperty("user.home"))

private val invalidCharsForFileName = arrayOf('"', '*', ':', '<', '>', '?', '\\', '|', Char(0x7F), '\u0000')

fun String.isValidFileName(): Boolean {
    return invalidCharsForFileName.none { contains(it) }
}

private val splitters = charArrayOf('\\' ,'/')

val String.lastPathSection get() = trim(*splitters).split(*splitters).last()
