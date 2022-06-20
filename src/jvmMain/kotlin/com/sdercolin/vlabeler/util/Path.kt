package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.model.LabelerConf.Companion.LabelerFileExtension
import java.io.File
import java.io.FilenameFilter

private const val AppNamePath = "vLabeler"
private const val AppConfFileName = "app.conf.json"
private const val LabelerFolderName = "labelers"

// Internal files
val ResourceDir get() = File(System.getProperty("compose.application.resources.dir"))
val DefaultAppConfFile get() = ResourceDir.resolve(AppConfFileName)
val DefaultLabelerDir get() = ResourceDir.resolve(LabelerFolderName)

// External files
val HomeDir get() = File(System.getProperty("user.home"))
val AppDir
    get() = when {
        isMacOS -> HomeDir.resolve("Library").resolve(AppNamePath)
        else -> HomeDir.resolve(AppNamePath)
    }
val CustomAppConfFile get() = AppDir.resolve(AppConfFileName)
val CustomLabelerDir get() = AppDir.resolve(LabelerFolderName)

private val labelerFileFilter = FilenameFilter { _, name -> name.endsWith(".$LabelerFileExtension") }
private fun getDefaultLabelers() = DefaultLabelerDir.listFiles(labelerFileFilter).orEmpty().toList()
private fun getCustomLabelers() = CustomLabelerDir.listFiles(labelerFileFilter).orEmpty().toList()
fun getAvailableLabelerFilesWithIsCustom(): List<Pair<File, Boolean>> =
    getDefaultLabelers().map { it to false } + getCustomLabelers().map { it to true }

private val invalidCharsForFileName = arrayOf('"', '*', ':', '<', '>', '?', '\\', '|', Char(0x7F), '\u0000')

fun String.isValidFileName(): Boolean {
    return invalidCharsForFileName.none { contains(it) }
}

private val splitters = charArrayOf('\\', '/')

val String.lastPathSection get() = trim(*splitters).split(*splitters).last()
