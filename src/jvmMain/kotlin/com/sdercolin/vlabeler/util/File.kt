package com.sdercolin.vlabeler.util

import java.io.File
import java.io.FilenameFilter

fun File.getDirectory(): File = if (isDirectory) this else parentFile
fun File.getChildren(filenameFilter: FilenameFilter? = null): List<File> =
    listFiles(filenameFilter)?.toList() ?: emptyList()

fun String.toFile(): File = File(this)
fun String.toFileOrNull(
    allowHomePlaceholder: Boolean = false,
    ensureExists: Boolean = true,
    ensureIsFile: Boolean = false,
    ensureIsDirectory: Boolean = false,
) = this.runIf(allowHomePlaceholder) { resolveHome() }
    .toFile()
    .runIfNotNull(ensureExists) { takeIf { it.exists() } }
    ?.runIfNotNull(ensureIsFile && ensureExists) { takeIf { it.isFile } }
    ?.runIfNotNull(ensureIsDirectory && ensureExists) { takeIf { it.isDirectory } }
