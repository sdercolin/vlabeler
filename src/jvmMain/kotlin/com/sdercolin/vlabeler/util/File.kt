package com.sdercolin.vlabeler.util

import java.io.File
import java.io.FilenameFilter
import java.nio.charset.Charset

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

fun File.readTextByEncoding(encoding: String?): String = readText(
    encoding?.let { Charset.forName(it) } ?: Charset.defaultCharset(),
).trim('\uFEFF')

fun File.findUnusedFile(base: String, existingAbsolutePaths: Set<String>): File {
    var result = base
    while (existingAbsolutePaths.contains(resolve(result).absolutePath)) {
        val firstPart = result.substringBeforeLast('.')
        val lastPart = result.substringAfterLast('.')
        if (lastPart.toIntOrNull() != null) {
            result = firstPart + "." + lastPart.toInt().inc().toString()
        } else {
            result += ".1"
        }
    }
    return resolve(result)
}
