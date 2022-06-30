package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.exception.EmptySampleDirectoryException
import com.sdercolin.vlabeler.io.fromRawLabels
import com.sdercolin.vlabeler.ui.editor.EditedEntry
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
    val currentEntryIndex: Int,
    val encoding: String? = null
) {

    val currentSampleFile: File
        get() = File(sampleDirectory).resolve("$currentSampleName.$SampleFileExtension")

    val currentEntry: Entry
        get() = getEntry()

    val entriesInCurrentSample: List<Entry>
        get() = entriesBySampleName.getValue(currentSampleName)

    val entriesWithSampleName: List<Pair<Entry, String>>
        get() = entriesBySampleName.flatMap { (sampleName, entries) ->
            entries.map { it to sampleName }
        }

    val allEntries = entriesBySampleName.flatMap { it.value }

    val totalEntryCount: Int
        get() = allEntries.size

    val currentEntryIndexInTotal: Int
        get() = allEntries.indexOf(currentEntry)

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    fun getEntry(sampleName: String = currentSampleName, index: Int = currentEntryIndex) =
        entriesBySampleName.getValue(sampleName)[index]

    fun getEntryForEditing(sampleName: String = currentSampleName, index: Int = currentEntryIndex) = EditedEntry(
        entry = getEntry(sampleName, index),
        sampleName = sampleName,
        index = index
    )

    fun updateEntry(editedEntry: EditedEntry): Project {
        val map = entriesBySampleName.toMutableMap()
        val entries = map.getValue(editedEntry.sampleName).toMutableList()
        entries[editedEntry.index] = editedEntry.entry
        if (labelerConf.continuous) {
            val previousIndex = editedEntry.index - 1
            entries.getOrNull(previousIndex)?.copy(end = editedEntry.entry.start)
                ?.let { entries[previousIndex] = it }
            val nextIndex = editedEntry.index + 1
            entries.getOrNull(nextIndex)?.copy(start = editedEntry.entry.end)
                ?.let { entries[nextIndex] = it }
        }
        map[editedEntry.sampleName] = entries.toList()
        return copy(entriesBySampleName = map.toMap())
    }

    fun renameEntry(sampleName: String, index: Int, newName: String): Project {
        val editedEntry = getEntryForEditing(sampleName, index)
        val renamed = editedEntry.entry.copy(name = newName)
        return updateEntry(editedEntry.edit(renamed))
    }

    fun duplicateEntry(sampleName: String, position: Int, newName: String): Project {
        val map = entriesBySampleName.toMutableMap()
        val entries = map.getValue(sampleName).toMutableList()
        var original = getEntry(sampleName, position)
        var duplicated = original.copy(name = newName)
        if (labelerConf.continuous) {
            val splitPoint = (original.start + original.end) / 2
            original = original.copy(end = splitPoint)
            duplicated = duplicated.copy(start = splitPoint)
            entries[position] = original
        }
        entries.add(position + 1, duplicated)
        map[sampleName] = entries.toList()
        return copy(entriesBySampleName = map.toMap())
    }

    fun removeCurrentEntry(): Project {
        val map = entriesBySampleName.toMutableMap()
        val entries = map.getValue(currentSampleName).toMutableList()
        val index = currentEntryIndex
        val removed = entries.removeAt(index)
        val newIndex = index - 1
        if (labelerConf.continuous) {
            val previousIndex = index - 1
            entries.getOrNull(previousIndex)?.copy(end = removed.end)
                ?.let { entries[previousIndex] = it }
        }
        map[currentSampleName] = entries.toList()
        return copy(entriesBySampleName = map.toMap(), currentEntryIndex = newIndex)
    }

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
            return copy(currentEntryIndex = entryBorderIndex)
        }
        val targetIndex = currentIndex + (if (reverse) -1 else 1)
        val targetName = sampleNames[targetIndex]
        val targetEntries = entriesBySampleName.getValue(targetName)
        val targetEntryIndex = if (reverse) targetEntries.lastIndex else 0
        return copy(currentSampleName = sampleNames[targetIndex], currentEntryIndex = targetEntryIndex)
    }

    fun hasSwitchedSample(previous: Project?) = previous != null && previous.currentSampleName != currentSampleName

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

            val inputFile = if (inputLabelFile != "") {
                File(inputLabelFile)
            } else null

            val entriesBySample = if (inputFile != null) {
                fromRawLabels(inputFile.readLines(Charset.forName(encoding)), labelerConf, sampleNames)
            } else {
                sampleNames.associateWith {
                    listOf(Entry.fromDefaultValues(it, labelerConf.defaultValues, labelerConf.defaultExtras))
                }
            }

            val project = Project(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                entriesBySampleName = entriesBySample,
                labelerConf = labelerConf,
                currentSampleName = sampleNames.first(),
                currentEntryIndex = 0,
                encoding = encoding
            )
            return Result.success(project)
        }
    }
}
