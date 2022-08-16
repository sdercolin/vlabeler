package com.sdercolin.vlabeler.util

import java.io.File
import java.io.FilenameFilter

fun File.getDirectory(): File = if (isDirectory) this else parentFile
fun File.getChildren(filenameFilter: FilenameFilter? = null): List<File> =
    listFiles(filenameFilter)?.toList() ?: emptyList()

fun String.toFile(): File = File(this)
fun String.toFileOrNull(
    allowHomePlaceholder: Boolean = false,
    ensureIsFile: Boolean = false,
    ensureIsDirectory: Boolean = false
) = this.runIf(allowHomePlaceholder) { resolveHome() }
    .toFile()
    .takeIf { it.exists() }
    ?.runIfNotNull(ensureIsFile) { takeIf { it.isFile } }
    ?.runIfNotNull(ensureIsDirectory) { takeIf { it.isDirectory } }
