package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isFileSystemCaseSensitive
import java.io.File
import java.io.FilenameFilter
import java.nio.charset.Charset

/**
 * Get the parent directory of a file, or the [File] itself if it is a directory.
 */
fun File.getDirectory(): File = if (isDirectory) this else parentFile

/**
 * Type-safe version of [File.listFiles].
 */
fun File.getChildren(filenameFilter: FilenameFilter? = null): List<File> =
    listFiles(filenameFilter)?.toList() ?: emptyList()

/**
 * File creation from a string, for better chain calls.
 */
fun String.toFile(): File = File(this)

/**
 * File creation from a string, for better chain calls. If the created [File] does not meet conditions, return null.
 *
 * @param allowHomePlaceholder Whether to allow the placeholder "~" in the path.
 * @param ensureExists Whether to ensure the [File] exists.
 * @param ensureIsFile Whether to ensure the [File] is an existing file.
 * @param ensureIsDirectory Whether to ensure the [File] is an existing directory.
 */
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

/**
 * Wrapper of [File.readText] with a default encoding, and trim the BOM.
 */
fun File.readTextByEncoding(encoding: String?): String = readText(
    encoding?.let { Charset.forName(it) } ?: Charset.defaultCharset(),
).trim('\uFEFF')

/**
 * Find a file name that is not existing in the given set of absolute paths by adding `.1`, `.2`, etc. to the base name.
 *
 * @param base The base name of the file.
 * @param existingAbsolutePaths The set of existing absolute paths.
 */
fun File.findUnusedFile(base: String, existingAbsolutePaths: Set<String>): File {
    val extension = base.substringAfterLast('.')
    var nameResult = base.substringBeforeLast('.')
    fun getResult() = nameResult + (if (extension.isEmpty()) "" else ".$extension")
    while (existingAbsolutePaths.contains(resolve(getResult()).absolutePath)) {
        val firstPart = nameResult.substringBeforeLast('.')
        val lastPart = nameResult.substringAfterLast('.')
        if (lastPart.toIntOrNull() != null) {
            nameResult = firstPart + "." + lastPart.toInt().inc().toString()
        } else {
            nameResult += ".1"
        }
    }
    return resolve(getResult())
}

/**
 * Check whether the given [File] is a child of this [File]. If any of the two [File]s is not absolute, return false.
 *
 * @param file The file to be checked as a child.
 */
fun File.containsFileRecursively(file: File): Boolean {
    if (file.isAbsolute.not() || isAbsolute.not()) return false
    if (file.absolutePath == absolutePath) return true
    val sections = absolutePath.split(File.separator)
    val childSections = file.absolutePath.split(File.separator)
    if (childSections.size <= sections.size) return false
    for (i in sections.indices) {
        if (sections[i] != childSections[i]) return false
    }
    return true
}

/**
 * Execute [File.deleteRecursively] and log if success.
 */
fun File.deleteRecursivelyLogged() {
    if (exists().not()) return
    val result = deleteRecursively()
    if (result) {
        Log.debug("Deleted recursively: $absolutePath")
    }
}

/**
 * Remove the directory if it is empty. (.DS_Store is ignored)
 */
fun File.removeDirectoryIfEmpty() {
    val children = listFiles().orEmpty()
    if (children.isEmpty()) {
        delete()
    } else {
        if (children.size == 1 && children.first().name == ".DS_Store") {
            children.first().deleteRecursively()
            delete()
        }
    }
}

/**
 * Compare file names with case sensitivity handled.
 */
fun String.equalsAsFileName(other: String): Boolean = if (isFileSystemCaseSensitive) {
    this == other
} else {
    this.equals(other, ignoreCase = true)
}

/**
 * Normalize file names with case sensitivity handled.
 */
fun String.asNormalizedFileName(): String = if (isFileSystemCaseSensitive) {
    this
} else {
    this.lowercase()
}
