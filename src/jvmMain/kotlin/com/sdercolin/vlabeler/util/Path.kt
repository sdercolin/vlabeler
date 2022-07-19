package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.isLinux
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.env.isWindows
import com.sdercolin.vlabeler.model.LabelerConf.Companion.LabelerFileExtension
import com.sdercolin.vlabeler.model.Project
import java.io.File
import java.io.FilenameFilter

private const val AppNamePath = "vLabeler"
private const val AppConfFileName = "app.conf.json"
private const val AppRecordFileName = "app.record.json"
private const val LabelerFolderName = "labelers"
private const val PluginFolderName = "plugins"
private const val RecordFolderName = ".record"
private const val ProjectCacheFolderNameSuffix = "_caches"

// Internal files
val ResourceDir get() = File(System.getProperty("compose.application.resources.dir"))
val DefaultAppConfFile get() = ResourceDir.resolve(AppConfFileName)
val DefaultLabelerDir get() = ResourceDir.resolve(LabelerFolderName)
val DefaultPluginDir get() = ResourceDir.resolve(PluginFolderName)
val RecordDir get() = if (isLinux) AppDir.resolve(RecordFolderName) else ResourceDir.resolve(RecordFolderName)
val AppRecordFile get() = RecordDir.resolve(AppRecordFileName)

// External files
val HomeDir get() = File(System.getProperty("user.home"))
val AppDir
    get() = when {
        isMacOS -> HomeDir.resolve("Library").resolve(AppNamePath)
        else -> HomeDir.resolve(AppNamePath)
    }
val CustomAppConfFile get() = AppDir.resolve(AppConfFileName)
val CustomLabelerDir get() = AppDir.resolve(LabelerFolderName)
val CustomPluginDir get() = AppDir.resolve(PluginFolderName)

private val labelerFileFilter = FilenameFilter { _, name -> name.endsWith(".$LabelerFileExtension") }
fun getDefaultLabelers() = DefaultLabelerDir.getChildren(labelerFileFilter)
fun getCustomLabelers() = CustomLabelerDir.getChildren(labelerFileFilter)

// Project files
fun Project.getCacheDir() = File(workingDirectory).resolve(projectFile.name + ProjectCacheFolderNameSuffix)

private val invalidCharsForFileName = arrayOf('"', '*', ':', '<', '>', '?', '\\', '|', Char(0x7F), '\u0000')

fun String.isValidFileName(): Boolean {
    return invalidCharsForFileName.none { contains(it) } && isNotBlank()
}

fun String.asPathRelativeToHome(): String = if (!isWindows && startsWith(HomeDir.absolutePath)) {
    "~" + drop(HomeDir.absolutePath.length)
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
