package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.exception.EmptySampleDirectoryException
import com.sdercolin.vlabeler.io.parseRawLabels
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.Charset

@Serializable
@Immutable
data class Project(
    val sampleDirectory: String,
    val workingDirectory: String,
    val projectName: String,
    val entriesBySampleName: Map<String, List<Entry>>,
    val labelerConf: LabelerConf,
    val currentSampleName: String,
    val currentEntryIndex: Int
) {

    val currentSampleFile: File
        get() = File(sampleDirectory).resolve("$currentSampleName.$SampleFileExtension")

    val currentEntry: Entry
        get() = entriesBySampleName.getValue(currentSampleName)[currentEntryIndex]

    private val allEntries = entriesBySampleName.flatMap { it.value }

    val totalEntryCount: Int
        get() = allEntries.size

    val currentEntryIndexInTotal: Int
        get() = allEntries.indexOf(currentEntry)

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    fun nextEntry() = switchEntry(reverse = false)
    fun previousEntry() = switchEntry(reverse = true)
    private fun switchEntry(reverse: Boolean): Project? {
        val currentSampleEntryBorderIndex = if (reverse) {
            0
        } else {
            entriesBySampleName.getValue(currentSampleName).lastIndex
        }
        if (currentEntryIndex == currentSampleEntryBorderIndex) {
            val newProject = switchSample(reverse, atEntryBorder = true) ?: return null
            val entriesInNewSample = entriesBySampleName.getValue(newProject.currentSampleName)
            val entryIndex = if (reverse) entriesInNewSample.lastIndex else 0
            return newProject.copy(currentEntryIndex = entryIndex)
        }
        val targetIndex = currentEntryIndex + (if (reverse) -1 else 1)
        return copy(currentEntryIndex = targetIndex)
    }

    fun nextSample() = switchSample(reverse = false)
    fun previousSample() = switchSample(reverse = true)
    private fun switchSample(reverse: Boolean, atEntryBorder: Boolean = false): Project? {
        val sampleNames = entriesBySampleName.keys.toList()
        val sampleBorderIndex = if (reverse) 0 else sampleNames.lastIndex
        val currentIndex = sampleNames.indexOf(currentSampleName)
        if (currentIndex == sampleBorderIndex) {
            if (atEntryBorder) return null
            val entryBorderIndex = if (reverse) {
                0
            } else {
                entriesBySampleName.getValue(currentSampleName).lastIndex
            }
            if (currentEntryIndex == entryBorderIndex) return null
            val targetIndex = currentEntryIndex + (if (reverse) -1 else 1)
            return copy(currentEntryIndex = targetIndex)
        }
        val targetIndex = currentIndex + (if (reverse) -1 else 1)
        val targetName = sampleNames[targetIndex]
        val targetEntries = entriesBySampleName.getValue(targetName)
        val targetEntryIndex = if (reverse) targetEntries.lastIndex else 0
        return copy(currentSampleName = sampleNames[targetIndex], currentEntryIndex = targetEntryIndex)
    }

    companion object {
        const val SampleFileExtension = "wav"
        const val ProjectFileExtension = "lbp"

        fun from(
            sampleDirectory: String,
            workingDirectory: String,
            projectName: String,
            labelerConf: LabelerConf,
            inputLabelFile: String,
            encoding: String
        ): Result<Project> {
            val sampleDirectoryFile = File(sampleDirectory)
            val sampleNames = sampleDirectoryFile.listFiles().orEmpty()
                .filter { it.extension == SampleFileExtension }
                .map { it.nameWithoutExtension }
                .sorted()

            if (sampleNames.isEmpty()) return Result.failure(EmptySampleDirectoryException())

            val parser = labelerConf.parser
            val inputFile = if (inputLabelFile != "") {
                File(inputLabelFile)
            } else null

            val entriesBySample = if (inputFile != null) {
                parseRawLabels(inputFile.readLines(Charset.forName(encoding)), parser!!)
            } else {
                val start = labelerConf.defaultValues.first()
                val end = labelerConf.defaultValues.last()
                val fields = labelerConf.defaultValues.drop(1).dropLast(1)
                sampleNames.associateWith {
                    listOf(Entry(it, start, end, fields))
                }
            }

            val project = Project(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                entriesBySampleName = entriesBySample,
                labelerConf = labelerConf,
                currentSampleName = sampleNames.first(),
                currentEntryIndex = 0
            )
            return Result.success(project)
        }
    }
}
