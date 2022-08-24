package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.exception.EmptySampleDirectoryException
import com.sdercolin.vlabeler.exception.InvalidCreatedProjectException
import com.sdercolin.vlabeler.io.fromRawLabels
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.orEmpty
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.nio.charset.Charset

@Serializable
@Immutable
data class Project(
    val sampleDirectory: String,
    val workingDirectory: String,
    val projectName: String,
    val cacheDirectory: String = getDefaultCacheDirectory(workingDirectory, projectName), // TODO: Remove migration code
    val entries: List<Entry>,
    val currentIndex: Int,
    val labelerConf: LabelerConf,
    val encoding: String? = null,
    val multipleEditMode: Boolean = labelerConf.continuous,
    val autoExportTargetPath: String? = null
) {
    @Transient
    private val entryIndexGroups: List<Pair<String, List<Int>>> = entries.indexGroupsConnected()

    @Transient
    private val entryGroups: List<Pair<String, List<Entry>>> = entries.entryGroupsConnected()

    @Transient
    val currentEntry: Entry = entries[currentIndex]

    @Transient
    private val currentGroupIndex: Int = getGroupIndex(currentIndex)

    @Transient
    val currentSampleName: String = currentEntry.sample

    val currentSampleFile: File
        get() = getSampleFile(currentSampleName)

    fun getSampleFile(sampleName: String) = File(sampleDirectory).resolve("$sampleName.$SampleFileExtension")

    @Transient
    val entryCount: Int = entries.size

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    val isUsingDefaultCacheDirectory get() = cacheDirectory == getDefaultCacheDirectory(workingDirectory, projectName)

    private fun getGroupIndex(entryIndex: Int) = entryIndexGroups.indexOfFirst { it.second.contains(entryIndex) }

    fun getEntriesForEditing(index: Int = currentIndex) = if (!multipleEditMode) {
        listOf(getEntryForEditing(index))
    } else {
        getEntriesInGroupForEditing(getGroupIndex(index))
    }

    private fun getEntryForEditing(index: Int = currentIndex) = IndexedEntry(
        entry = entries[index],
        index = index,
    )

    fun getEntriesInGroupForEditing(groupIndex: Int = currentGroupIndex) = entryIndexGroups[groupIndex].second
        .map {
            IndexedEntry(
                entry = entries[it],
                index = it,
            )
        }

    fun updateOnLoadedSample(sampleInfo: SampleInfo): Project {
        val entries = entries.toMutableList()
        val changedEntries = entries.withIndex()
            .filter { it.value.sample == sampleInfo.name }
            .filter { it.value.end <= 0f }
            .map {
                val end = sampleInfo.lengthMillis + it.value.end
                it.copy(value = it.value.copy(end = end))
            }
        if (changedEntries.isEmpty()) return this
        changedEntries.forEach { entries[it.index] = it.value }
        return copy(entries = entries)
    }

    fun updateEntries(editedEntries: List<IndexedEntry>): Project {
        val entries = entries.toMutableList()
        if (labelerConf.continuous) {
            val previousIndex = editedEntries.first().index - 1
            entries.getOrNull(previousIndex)
                ?.takeIf { it.sample == editedEntries.first().sample }
                ?.copy(end = editedEntries.first().start)
                ?.let { entries[previousIndex] = it }
            val nextIndex = editedEntries.last().index + 1
            entries.getOrNull(nextIndex)
                ?.takeIf { it.sample == editedEntries.last().sample }
                ?.copy(start = editedEntries.last().end)
                ?.let { entries[nextIndex] = it }
        }
        editedEntries.forEach {
            entries[it.index] = it.entry
        }
        return copy(entries = entries)
    }

    private fun updateEntry(editedEntry: IndexedEntry) = updateEntries(listOf(editedEntry))

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

    fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?): Project {
        val entries = entries.toMutableList()
        val entry = entries[index]
        val editedCurrentEntry = entry.copy(
            name = rename ?: entry.name,
            end = position,
            points = entry.points.map { it.coerceAtMost(position) },
        )
        val newEntry = entry.copy(
            name = newName,
            start = position,
            points = entry.points.map { it.coerceAtLeast(position) },
        )
        entries[index] = editedCurrentEntry
        entries.add(index + 1, newEntry)
        val newIndex = targetEntryIndex ?: index
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

    fun validate(): Project {

        // Check multiMode enabled
        if (multipleEditMode) require(
            labelerConf.continuous,
        ) { "Multi-entry mode can only be used in continuous labelers." }

        // Check currentIndex valid
        requireNotNull(entries.getOrNull(currentIndex)) { "Invalid currentIndex: $currentIndex" }

        // Check continuous
        if (labelerConf.continuous) {
            entryGroups.forEach { (_, entries) ->
                entries.zipWithNext().forEach {
                    require(it.first.end == it.second.start) {
                        "Not continuous between entries: $it"
                    }
                }
            }
        }

        // check entry name duplicates
        if (!labelerConf.allowSameNameEntry) {
            val names = entries.map { it.name }
            require(names.distinct().size == names.size) { "Duplicate entry names found." }
        }

        // Check points
        val entries = entries.map {
            require(it.points.size == labelerConf.fields.size) {
                "Point size doesn't match in entry: $it. Required point size = ${labelerConf.fields.size}"
            }
            require(it.extras.size == labelerConf.extraFieldNames.size) {
                "Extra size doesn't match in entry: $it. Required extra size = ${labelerConf.extraFieldNames.size}"
            }
            if (it.end > 0) require(it.start <= it.end) {
                "Start is greater than end in entry: $it"
            }
            var entryResult = it
            it.points.forEachIndexed { index, point ->
                runCatching {
                    require(point >= entryResult.start) {
                        "Point $point is smaller than start in entry: $it"
                    }
                }.onFailure { t ->
                    when (labelerConf.overflowBeforeStart) {
                        LabelerConf.PointOverflow.AdjustBorder -> {
                            entryResult = entryResult.copy(start = point)
                        }
                        LabelerConf.PointOverflow.AdjustPoint -> {
                            val points = entryResult.points.toMutableList()
                            points[index] = entryResult.start
                            entryResult = entryResult.copy(points = points)
                        }
                        LabelerConf.PointOverflow.Error -> throw t
                    }
                }
                if (it.end > 0) {
                    runCatching {
                        require(point <= it.end) {
                            "Point $point is greater than end in entry: $it"
                        }
                    }.onFailure { t ->
                        when (labelerConf.overflowAfterEnd) {
                            LabelerConf.PointOverflow.AdjustBorder -> {
                                entryResult = entryResult.copy(end = point)
                            }
                            LabelerConf.PointOverflow.AdjustPoint -> {
                                val points = entryResult.points.toMutableList()
                                points[index] = entryResult.end
                                entryResult = entryResult.copy(points = points)
                            }
                            LabelerConf.PointOverflow.Error -> throw t
                        }
                    }
                }
            }
            entryResult
        }

        return copy(entries = entries)
    }

    companion object {
        const val SampleFileExtension = "wav"
        const val ProjectFileExtension = "lbp"
        private const val DefaultCacheDirectorySuffix = ".$ProjectFileExtension.caches"

        fun getDefaultCacheDirectory(location: String, projectName: String): String {
            return File(location, "$projectName$DefaultCacheDirectorySuffix").absolutePath
        }
    }
}

private fun generateEntriesByPlugin(
    labelerConf: LabelerConf,
    sampleNames: List<String>,
    plugin: Plugin,
    params: ParamMap?,
    inputFile: File?,
    encoding: String
): Result<List<Entry>> = runCatching {
    when (
        val result =
            runTemplatePlugin(plugin, params.orEmpty(), listOfNotNull(inputFile), encoding, sampleNames, labelerConf)
    ) {
        is TemplatePluginResult.Parsed -> {
            val entries = result.entries.map {
                it.copy(
                    points = it.points.take(labelerConf.fields.count()),
                    extras = it.extras.take(labelerConf.extraFieldNames.count()),
                )
            }.map { it.toEntry(fallbackSample = sampleNames.first()) }

            entries.postApplyLabelerConf(labelerConf)
        }
        is TemplatePluginResult.Raw -> fromRawLabels(result.lines, inputFile, labelerConf, sampleNames)
    }
}

fun List<Entry>.postApplyLabelerConf(
    labelerConf: LabelerConf
): List<Entry> = toContinuous(labelerConf.continuous)
    .distinct(labelerConf.allowSameNameEntry)

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

private fun List<Entry>.distinct(allowDuplicated: Boolean): List<Entry> {
    if (allowDuplicated) return this
    return distinctBy { it.name }
}

/**
 * Should be called from IO threads, because this function runs scripting and may take time
 */
@Suppress("RedundantSuspendModifier")
suspend fun projectOf(
    sampleDirectory: String,
    workingDirectory: String,
    projectName: String,
    cacheDirectory: String,
    labelerConf: LabelerConf,
    plugin: Plugin?,
    pluginParams: ParamMap?,
    inputFilePath: String,
    encoding: String,
    autoExport: Boolean
): Result<Project> {
    val sampleDirectoryFile = File(sampleDirectory)
    val sampleNames = sampleDirectoryFile.getChildren()
        .filter { it.extension == Project.SampleFileExtension }
        .map { it.nameWithoutExtension }
        .sorted()

    if (sampleNames.isEmpty()) return Result.failure(EmptySampleDirectoryException())

    val inputFile = if (inputFilePath != "") {
        File(inputFilePath)
    } else null

    val entries = when {
        plugin != null -> {
            generateEntriesByPlugin(labelerConf, sampleNames, plugin, pluginParams, inputFile, encoding)
                .getOrElse {
                    return Result.failure(it)
                }
        }
        inputFile != null -> {
            fromRawLabels(
                inputFile.readLines(Charset.forName(encoding)),
                inputFile,
                labelerConf,
                sampleNames,
            )
        }
        else -> {
            sampleNames.map {
                Entry.fromDefaultValues(it, it, labelerConf)
            }
        }
    }

    val autoExportTargetPath = if (!autoExport) {
        null
    } else {
        if (inputFile != null && plugin == null) {
            inputFile.absolutePath
        } else {
            labelerConf.defaultInputFilePath?.let { File(sampleDirectory).resolve(it) }?.absolutePath
        }
    }

    return runCatching {
        require(entries.isNotEmpty()) {
            "No entries found"
        }
        Project(
            sampleDirectory = sampleDirectory,
            workingDirectory = workingDirectory,
            projectName = projectName,
            cacheDirectory = cacheDirectory,
            entries = entries,
            currentIndex = 0,
            labelerConf = labelerConf,
            encoding = encoding,
            autoExportTargetPath = autoExportTargetPath,
        ).validate()
    }.onFailure {
        return Result.failure(InvalidCreatedProjectException(it))
    }
}
