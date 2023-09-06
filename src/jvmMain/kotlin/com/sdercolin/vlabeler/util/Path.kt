package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.env.isWindows
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.ConvertedAudioRepository
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import java.io.File

private const val APP_NAME_PATH = "vLabeler"
private const val APP_CONF_FILE_NAME = "app.conf.json"
private const val APP_RECORD_FILE_NAME = "app.record.json"
private const val LABELER_FOLDER_NAME = "labelers"
private const val PLUGIN_FOLDER_NAME = "plugins"
private const val RECORD_FOLDER_NAME = ".record"

// Internal files
val ResourcePath: String? get() = System.getProperty("compose.application.resources.dir")
val ResourceDir get() = File(requireNotNull(ResourcePath))
val DefaultAppConfFile get() = ResourceDir.resolve(APP_CONF_FILE_NAME)
val DefaultLabelerDir get() = ResourceDir.resolve(LABELER_FOLDER_NAME)
val DefaultPluginDir get() = ResourceDir.resolve(PLUGIN_FOLDER_NAME)

// External files
val HomeDir get() = File(System.getProperty("user.home"))
val AppDir
    get() = when {
        isMacOS -> HomeDir.resolve("Library").resolve(APP_NAME_PATH)
        else -> HomeDir.resolve(APP_NAME_PATH)
    }
val CustomAppConfFile get() = AppDir.resolve(APP_CONF_FILE_NAME)
val CustomLabelerDir get() = AppDir.resolve(LABELER_FOLDER_NAME)
val CustomPluginDir get() = AppDir.resolve(PLUGIN_FOLDER_NAME)
val RecordDir get() = AppDir.resolve(RECORD_FOLDER_NAME)
val AppRecordFile get() = RecordDir.resolve(APP_RECORD_FILE_NAME)
val DefaultDownloadDir: File
    get() = listOf(
        "Downloads",
        "Download",
        "Desktop",
    ).find { HomeDir.resolve(it).absolutePath.toFileOrNull(ensureIsDirectory = true) != null }
        ?.let { HomeDir.resolve(it) }
        ?: HomeDir

// Project files
fun Project.getCacheDir() = cacheDirectory
fun Project.moveCacheDirTo(newDirectory: File, clearOld: Boolean = true) {
    ChartRepository.moveTo(cacheDirectory, newDirectory, clearOld)
    SampleInfoRepository.moveTo(cacheDirectory, newDirectory, clearOld)
    ConvertedAudioRepository.moveTo(cacheDirectory, newDirectory, clearOld)
    if (clearOld) cacheDirectory.removeDirectoryIfEmpty()
}

fun Project.clearCache() {
    ChartRepository.clear(this)
    SampleInfoRepository.clear(this)
    ConvertedAudioRepository.clear(this)
    cacheDirectory.removeDirectoryIfEmpty()
}

private val invalidCharsForFileName = arrayOf('"', '*', ':', '<', '>', '?', '\\', '/', '|', Char(0x7F), '\u0000')

fun String.isValidFileName(): Boolean {
    return invalidCharsForFileName.none { contains(it) } && isNotBlank()
}

fun String.asPathRelativeToHome(): String = if (!isWindows && startsWith(HomeDir.absolutePath)) {
    "~" + drop(HomeDir.absolutePath.length)
} else {
    this
}

fun String.resolveHome(): String = if (startsWith("~")) {
    HomeDir.absolutePath + drop(1)
} else {
    this
}

private val splitters = charArrayOf('\\', '/')

val String.pathSections get() = trim(*splitters).split(*splitters)
val String.lastPathSection get() = pathSections.last()

fun List<String>.asSimplifiedPaths(): List<String> {
    val pathsInSection = map { it.pathSections }
    val result = pathsInSection.map { it.last() }.toMutableList()
    while (result.distinct().size != result.size) {
        val startIndex = result.indexOfFirst { path -> result.count { it == path } > 1 }
        val indexes = indices.filter { result[it] == result[startIndex] }
        val countOfSplitters = result[startIndex].count { it in splitters }
        for (index in indexes) {
            val sections = pathsInSection[index]
            result[index] = sections[sections.lastIndex - 1 - countOfSplitters] + "/" + result[index]
        }
    }
    for (index in indices) {
        if (result[index].contains("/")) {
            if (result[index].startsWith(pathsInSection[index].first()).not()) {
                result[index] = ".../" + result[index]
            }
        }
    }
    return result
}
