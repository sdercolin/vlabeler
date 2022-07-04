package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.EmptySampleDirectoryException
import com.sdercolin.vlabeler.io.fromRawLabels
import com.sdercolin.vlabeler.ui.editor.EditedEntry
import com.sdercolin.vlabeler.util.ParamMap
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.Charset

@Serializable
@Immutable
data class Project(
    val sampleDirectory: String,
    val workingDirectory: String,
    val projectName: String,
    val entries: List<Entry>,
    val currentIndex: Int,
    val labelerConf: LabelerConf,
    val encoding: String? = null
) {

    val entryIndexGroups: List<Pair<String, List<Int>>> = entries.indexGroupsConnected()
    val entryGroups: List<Pair<String, List<Entry>>> = entries.entryGroupsConnected()
    val currentEntry: Entry = entries[currentIndex]
    val currentGroupIndex: Int = getGroupIndex(currentIndex)
    val currentEntryGroup: List<Entry> = entryGroups[currentGroupIndex].second
    val currentSampleName: String = currentEntry.sample

    val currentSampleFile: File // TODO: nullable if not existing
        get() = File(sampleDirectory).resolve("$currentSampleName.$SampleFileExtension")

    val entryCount: Int = entries.size

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    fun getGroupIndex(entryIndex: Int) = entryIndexGroups.indexOfFirst { it.second.contains(entryIndex) }

    fun getEntryForEditing(index: Int = currentIndex) = EditedEntry(
        entry = entries[index],
        index = index
    )

    fun updateEntry(editedEntry: EditedEntry): Project {
        val entries = entries.toMutableList()
        if (labelerConf.continuous) {
            val previousIndex = editedEntry.index - 1
            entries.getOrNull(previousIndex)
                ?.takeIf { it.sample == editedEntry.entry.sample }
                ?.copy(end = editedEntry.entry.start)
                ?.let { entries[previousIndex] = it }
            val nextIndex = editedEntry.index + 1
            entries.getOrNull(nextIndex)
                ?.takeIf { it.sample == editedEntry.entry.sample }
                ?.copy(start = editedEntry.entry.end)
                ?.let { entries[nextIndex] = it }
        }
        entries[editedEntry.index] = editedEntry.entry
        return copy(entries = entries)
    }

    fun renameEntry(index: Int, newName: String): Project {
        val editedEntry = getEntryForEditing(index)
        val renamed = editedEntry.entry.copy(name = newName)
        return updateEntry(editedEntry.edit(renamed))
    }

    fun duplicateEntry(index: Int, newName: String): Project {
        val entries = entries.toMutableList()
        var original = entries[index]
        var duplicated = original.copy(name = newName)
        if (labelerConf.continuous) {
            val splitPoint = (original.start + original.end) / 2
            original = original.copy(end = splitPoint)
            duplicated = duplicated.copy(start = splitPoint)
            entries[index] = original
        }
        entries.add(index + 1, duplicated)
        return copy(entries = entries, currentIndex = index)
    }

    fun removeCurrentEntry(): Project {
        val index = currentIndex
        val entries = entries.toMutableList()
        val removed = requireNotNull(entries.removeAt(index))
        val newIndex = index - 1
        if (labelerConf.continuous) {
            val previousIndex = index - 1
            entries.getOrNull(previousIndex)
                ?.takeIf { it.sample == removed.sample }
                ?.copy(end = removed.end)
                ?.let { entries[previousIndex] = it }
        }
        return copy(entries = entries, currentIndex = newIndex)
    }

    fun nextEntry() = switchEntry(reverse = false)
    fun previousEntry() = switchEntry(reverse = true)
    private fun switchEntry(reverse: Boolean): Project {
        val targetIndex = (currentIndex + if (reverse) -1 else 1).coerceIn(0..entries.lastIndex)
        return copy(currentIndex = targetIndex)
    }

    fun nextSample() = switchSample(reverse = false)
    fun previousSample() = switchSample(reverse = true)
    private fun switchSample(reverse: Boolean): Project {
        val currentGroupIndex = getGroupIndex(currentIndex)
        val targetGroupIndex = currentGroupIndex + if (reverse) -1 else 1
        val targetEntryIndex = when {
            targetGroupIndex < 0 -> 0
            targetGroupIndex > entryGroups.lastIndex -> entries.lastIndex
            else -> {
                val indexesInTargetGroup = entryIndexGroups[targetGroupIndex].second
                if (reverse) indexesInTargetGroup.last() else indexesInTargetGroup.first()
            }
        }
        return copy(currentIndex = targetEntryIndex)
    }

    fun hasSwitchedSample(previous: Project?) = previous != null && previous.currentSampleName != currentSampleName

    companion object {
        const val SampleFileExtension = "wav"
        const val ProjectFileExtension = "lbp"
    }
}

private fun generateEntriesByPlugin(
    labelerConf: LabelerConf,
    sampleNames: List<String>,
    plugin: Plugin,
    params: ParamMap?,
    inputFile: File?,
    encoding: String
): List<Entry> {
    val entries = runTemplatePlugin(plugin, params.orEmpty(), listOfNotNull(inputFile), encoding, sampleNames)
        .map {
            it.copy(
                points = it.points.take(labelerConf.fields.count()),
                extra = it.extra.take(labelerConf.extraFieldNames.count())
            )
        }
        .map { it.toEntry(sampleName = it.sample ?: sampleNames.first()) }
    return mergeEntriesWithSampleNames(labelerConf, entries, sampleNames)
}

fun mergeEntriesWithSampleNames(
    labelerConf: LabelerConf,
    entries: List<Entry>,
    sampleNames: List<String>
): List<Entry> {
    val sampleNameUsed = entries.map { it.sample }.toSet()
    val sampleNamesNotUsed = sampleNames.filterNot { it in sampleNameUsed }
    val additionalEntries = sampleNamesNotUsed.map { sampleName ->
        Entry.fromDefaultValues(sampleName, sampleName, labelerConf.defaultValues, labelerConf.defaultExtras)
            .also {
                Log.info("Sample $sampleName doesn't have entries, created default: $it")
            }
        // TODO: notify
    }
    return (entries + additionalEntries).toContinuous(labelerConf.continuous)
}

private fun List<Entry>.indexGroupsConnected(): List<Pair<String, List<Int>>> = withIndex()
    .fold(listOf<Pair<String, MutableList<IndexedValue<Entry>>>>()) { acc, entry ->
        val lastGroup = acc.lastOrNull()
        if (lastGroup == null || lastGroup.first != entry.value.sample) {
            acc.plus(entry.value.sample to mutableListOf(entry))
        } else {
            lastGroup.second.add(entry)
            acc
        }
    }.map { group -> group.first to group.second.map { it.index } }

private fun List<Entry>.entryGroupsConnected() = indexGroupsConnected().map { group ->
    group.first to group.second.map { this[it] }
}

private fun List<Entry>.toContinuous(continuous: Boolean): List<Entry> {
    if (!continuous) return this
    return entryGroupsConnected()
        .flatMap { (_, entries) ->
            entries
                .sortedBy { it.start }
                .distinctBy { it.start }
                .let {
                    it.zipWithNext { current, next ->
                        current.copy(end = next.start)
                    }.plus(it.last())
                }
        }
}

/**
 * Should be called from IO threads, because this function runs scripting and may take time
 */
@Suppress("RedundantSuspendModifier")
suspend fun projectOf(
    sampleDirectory: String,
    workingDirectory: String,
    projectName: String,
    labelerConf: LabelerConf,
    plugin: Plugin?,
    pluginParams: ParamMap?,
    inputFilePath: String,
    encoding: String
): Result<Project> {
    val sampleDirectoryFile = File(sampleDirectory)
    val sampleNames = sampleDirectoryFile.listFiles().orEmpty()
        .filter { it.extension == Project.SampleFileExtension }
        .map { it.nameWithoutExtension }
        .sorted()

    if (sampleNames.isEmpty()) return Result.failure(EmptySampleDirectoryException())

    val inputFile = if (inputFilePath != "") {
        File(inputFilePath)
    } else null

    val entries = when {
        plugin != null -> generateEntriesByPlugin(labelerConf, sampleNames, plugin, pluginParams, inputFile, encoding)
        inputFile != null -> {
            fromRawLabels(inputFile.readLines(Charset.forName(encoding)), labelerConf, sampleNames)
        }
        else -> {
            sampleNames.map {
                Entry.fromDefaultValues(it, it, labelerConf.defaultValues, labelerConf.defaultExtras)
            }
        }
    }

    val project = Project(
        sampleDirectory = sampleDirectory,
        workingDirectory = workingDirectory,
        projectName = projectName,
        entries = entries,
        currentIndex = 0,
        labelerConf = labelerConf,
        encoding = encoding
    )
    return Result.success(project)
}
