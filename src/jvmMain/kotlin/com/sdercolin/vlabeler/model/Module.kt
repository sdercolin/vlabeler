package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

/**
 * A subproject of a [Project] containing a list of [Entry]s. All basic operations on entries are done inside a single
 * module.
 *
 * @property name The unique name of the module. If "" (empty string), the module is displayed as `(Root)`. For labelers
 *     that do not support multiple modules, this should always be "".
 * @property sampleDirectoryPath The path of the sample directory. Should always be under [Project.rootSampleDirectory].
 *     Basically, it's stored as a relative path to [Project.rootSampleDirectory]. Use [getSampleDirectory] to get the
 *     actual directory.
 * @property entries The list of entries in the module.
 * @property currentIndex The index of the current entry in [entries].
 * @property rawFilePath The path of the target raw label file. Should always be under [Project.rootSampleDirectory].
 *     Basically, it's stored as a relative path to [Project.rootSampleDirectory]. Use [getRawFile] to get the actual
 *     file.
 * @property entryFilter The filter that is applied to the entries.
 */
@Serializable
@Immutable
data class Module(
    val name: String,
    @SerialName("sampleDirectory")
    val sampleDirectoryPath: String,
    val entries: List<Entry>,
    val currentIndex: Int,
    val rawFilePath: String? = null,
    val entryFilter: EntryFilter? = null,
) {

    constructor(
        rootDirectory: File,
        name: String,
        sampleDirectory: File,
        entries: List<Entry>,
        currentIndex: Int,
        rawFilePath: File? = null,
        entryFilter: EntryFilter? = null,
    ) : this(
        name = name,
        sampleDirectoryPath = if (sampleDirectory.isAbsolute) {
            sampleDirectory.relativeTo(rootDirectory).path
        } else {
            sampleDirectory.path
        },
        entries = entries,
        currentIndex = currentIndex,
        rawFilePath = if (rawFilePath?.isAbsolute == true) {
            rawFilePath.relativeTo(rootDirectory).path
        } else {
            rawFilePath?.path
        },
        entryFilter = entryFilter,
    )

    @Transient
    private val filteredEntryIndexes: List<Int> =
        entries.indices.filter { entryFilter?.matches(entries[it]) ?: true }

    @Transient
    private val entryIndexGroups: List<Pair<String, List<Int>>> = entries.indexGroupsConnected()

    @Transient
    private val filteredEntryIndexGroupIndexes: List<Pair<Int, List<Int>>> = entryIndexGroups
        .mapIndexed { index, pair -> index to pair.second }
        .map { (groupIndex, entryIndexes) ->
            groupIndex to entryIndexes.filter { entryFilter?.matches(entries[it]) ?: true }
        }
        .filter { it.second.isNotEmpty() }

    @Transient
    val entryGroups: List<Pair<String, List<IndexedEntry>>> = entries.entryGroupsConnected()

    val currentEntryGroup get() = entryGroups[currentGroupIndex].second

    val currentEntry: Entry
        get() = entries[currentIndex]

    private val currentGroupIndex: Int
        get() = getGroupIndex(currentIndex)

    private val currentSampleName: String
        get() = currentEntry.sample

    fun getCurrentSampleFile(project: Project): File = getSampleFile(project, currentSampleName)

    fun getSampleDirectory(project: Project) =
        project.rootSampleDirectory.resolve(sampleDirectoryPath)

    fun getSampleFile(project: Project, sampleName: String): File {
        return getSampleDirectory(project).resolve(sampleName)
    }

    fun getRawFile(project: Project) = rawFilePath?.let { project.rootSampleDirectory.resolve(it) }

    @Transient
    val entryCount: Int = entries.size
    private fun getGroupIndex(entryIndex: Int) = entryIndexGroups.indexOfFirst { it.second.contains(entryIndex) }

    fun getEntriesForEditing(index: Int?, multipleEditMode: Boolean) = if (!multipleEditMode) {
        listOf(getEntryForEditing(index ?: currentIndex))
    } else {
        getEntriesInGroupForEditing(getGroupIndex(index ?: currentIndex))
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

    fun updateOnLoadedSample(sampleInfo: SampleInfo): Module {
        val entries = entries.toMutableList()
        val changedEntries = entries.withIndex()
            .filter { it.value.sample == sampleInfo.name }
            .filter { entry ->
                (entry.value.needSync && entry.value.end == 0f) || (entry.value.end < 0f)
            }
            .map {
                val end = sampleInfo.lengthMillis + it.value.end
                it.copy(value = it.value.copy(end = end, needSync = false))
            }
        if (changedEntries.isEmpty()) return this
        changedEntries.forEach { entries[it.index] = it.value }
        return copy(entries = entries)
    }

    fun updateEntries(editedEntries: List<IndexedEntry>, labelerConf: LabelerConf): Module {
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

    fun updateCurrentEntry(entry: Entry, labelerConf: LabelerConf): Module {
        val editedEntry = getEntryForEditing(currentIndex)
        return updateEntry(editedEntry.edit(entry), labelerConf)
    }

    private fun updateEntry(editedEntry: IndexedEntry, labelerConf: LabelerConf) =
        updateEntries(listOf(editedEntry), labelerConf)

    fun takePostEditAction(
        editions: List<Edition>,
        editorConf: AppConf.Editor,
        actionType: AppConf.PostEditAction.Type,
        labelerConf: LabelerConf,
    ): Module {
        val entries = entries.toMutableList()
        var currentIndex = currentIndex
        val conf = when (actionType) {
            AppConf.PostEditAction.Type.Next -> editorConf.postEditNext
            AppConf.PostEditAction.Type.Done -> editorConf.postEditDone
        }
        if (!conf.enabled) return this
        val acceptedFieldNames = when (conf.field) {
            AppConf.PostEditAction.TriggerField.UseLabeler -> when (actionType) {
                AppConf.PostEditAction.Type.Next -> labelerConf.postEditNextTriggerFieldNames
                AppConf.PostEditAction.Type.Done -> labelerConf.postEditDoneTriggerFieldNames
            }
            AppConf.PostEditAction.TriggerField.UseStart -> listOf("start")
            AppConf.PostEditAction.TriggerField.UseEnd -> listOf("end")
            AppConf.PostEditAction.TriggerField.UseAny -> null
        }
        val acceptedMethod = listOfNotNull(
            Edition.Method.Dragging.takeIf { conf.useDragging },
            Edition.Method.SetWithCursor.takeIf { conf.useCursorSet },
        )
        if (acceptedFieldNames?.isEmpty() == true || acceptedMethod.isEmpty()) return this
        editions.forEach { edition ->
            if (edition.method !in acceptedMethod) return@forEach
            if (acceptedFieldNames != null && edition.fieldNames.intersect(acceptedFieldNames).isEmpty()) return@forEach
            when (actionType) {
                AppConf.PostEditAction.Type.Next -> {
                    currentIndex = (edition.index + 1).coerceAtMost(entries.lastIndex)
                }
                AppConf.PostEditAction.Type.Done -> {
                    entries[edition.index] = entries[edition.index].done()
                }
            }
        }
        return copy(entries = entries, currentIndex = currentIndex)
    }

    fun renameEntry(index: Int, newName: String, labelerConf: LabelerConf): Module {
        val editedEntry = getEntryForEditing(index)
        val renamed = editedEntry.entry.copy(name = newName)
        return updateEntry(editedEntry.edit(renamed), labelerConf)
    }

    fun duplicateEntry(index: Int, newName: String, labelerConf: LabelerConf): Module {
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

    fun removeCurrentEntry(labelerConf: LabelerConf): Module {
        val index = currentIndex
        val entries = entries.toMutableList()
        val removed = requireNotNull(entries.removeAt(index))
        val newIndex = (index - 1).coerceAtLeast(0)
        if (labelerConf.continuous) {
            val previousIndex = index - 1
            entries.getOrNull(previousIndex)
                ?.takeIf { it.sample == removed.sample }
                ?.copy(end = removed.end)
                ?.let { entries[previousIndex] = it }
        }
        return copy(entries = entries, currentIndex = newIndex)
    }

    fun cutEntry(index: Int, position: Float, rename: String?, newName: String, targetEntryIndex: Int?): Module {
        val entries = entries.toMutableList()
        val entry = entries[index]
        val editedCurrentEntry = entry.copy(
            name = rename ?: entry.name,
            end = position,
            points = entry.points.map { it.coerceAtMost(position) },
            notes = entry.notes.copy(done = true),
        )
        val newEntry = entry.copy(
            name = newName,
            start = position,
            points = entry.points.map { it.coerceAtLeast(position) },
            notes = entry.notes.copy(done = true),
        )
        entries[index] = editedCurrentEntry
        entries.add(index + 1, newEntry)
        val newIndex = targetEntryIndex ?: index
        return copy(entries = entries, currentIndex = newIndex)
    }

    fun nextEntry() = switchEntry(reverse = false)
    fun previousEntry() = switchEntry(reverse = true)
    private fun switchEntry(reverse: Boolean): Module {
        val targetIndex = if (reverse) {
            filteredEntryIndexes.lastOrNull { it < currentIndex } ?: currentIndex
        } else {
            filteredEntryIndexes.firstOrNull { it > currentIndex } ?: currentIndex
        }
        return copy(currentIndex = targetIndex)
    }

    fun nextSample() = switchSample(reverse = false)
    fun previousSample() = switchSample(reverse = true)
    private fun switchSample(reverse: Boolean): Module {
        val currentGroupIndex = getGroupIndex(currentIndex)
        val targetGroupIndex = if (reverse) {
            filteredEntryIndexGroupIndexes.lastOrNull { it.first < currentGroupIndex }?.first ?: currentGroupIndex
        } else {
            filteredEntryIndexGroupIndexes.firstOrNull { it.first > currentGroupIndex }?.first ?: currentGroupIndex
        }
        val targetEntryIndex = if (targetGroupIndex == currentGroupIndex) {
            val indexesInCurrentGroup = filteredEntryIndexGroupIndexes.first { it.first == targetGroupIndex }.second
            if (reverse) indexesInCurrentGroup.first() else indexesInCurrentGroup.last()
        } else {
            val indexesInTargetGroup = filteredEntryIndexGroupIndexes.first { it.first == targetGroupIndex }.second
            if (reverse) indexesInTargetGroup.last() else indexesInTargetGroup.first()
        }
        return copy(currentIndex = targetEntryIndex)
    }

    fun hasSwitchedSample(previous: Module?) = previous != null && previous.currentSampleName != currentSampleName

    fun toggleEntryDone(index: Int): Module {
        val entry = entries[index]
        val editedEntry = entry.doneToggled()
        return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
    }

    fun toggleEntryStar(index: Int): Module {
        val entry = entries[index]
        val editedEntry = entry.starToggled()
        return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
    }

    fun editEntryTag(index: Int, tag: String): Module {
        val entry = entries[index]
        val editedEntry = entry.tagEdited(tag)
        return copy(entries = entries.toMutableList().apply { this[index] = editedEntry })
    }

    fun validate(multipleEditMode: Boolean, labelerConf: LabelerConf): Module {
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

        // Check points
        val entries = entries.map {
            require(it.points.size == labelerConf.fields.size) {
                "Point size doesn't match in entry: $it. Required point size = ${labelerConf.fields.size}"
            }

            require(it.extras.size == labelerConf.extraFields.size) {
                "Extra size doesn't match in entry: $it. Required extra size = ${labelerConf.extraFields.size}"
            }
            labelerConf.extraFields.forEachIndexed { index, extraField ->
                if (!extraField.isOptional) {
                    require(it.extras[index] != null) {
                        "Extra field ${extraField.name} is not optional, but is null in entry: $it"
                    }
                }
            }

            var entryResult = it

            if (it.start < 0) {
                entryResult = entryResult.copy(start = 0f)
            }
            if (entryResult.end > 0 && entryResult.start > entryResult.end) {
                entryResult = entryResult.copy(end = entryResult.start)
            }

            // do not check right border, because we don't know the length of the audio file

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
                        require(point <= entryResult.end) {
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

    fun isParallelTo(other: Module) = this != other && this.rawFilePath == other.rawFilePath && this.rawFilePath != null
}

fun List<Entry>.postApplyLabelerConf(
    labelerConf: LabelerConf,
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
    group.first to group.second.map { IndexedEntry(entry = this[it], index = it) }
}

private fun List<Entry>.toContinuous(continuous: Boolean): List<Entry> {
    if (!continuous) return this
    return entryGroupsConnected()
        .flatMap { (_, entries) ->
            entries
                .map { it.entry }
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
 * A data class for serializing and deserializing [Module] to and from the JavaScript environment. Should be consistent
 * with the JavaScript class defined in `class_module.js`.
 */
@Serializable
data class JsModule(
    val name: String,
    val sampleDirectory: String,
    val entries: List<Entry>,
    val currentIndex: Int,
    val rawFilePath: String?,
    val entryFilter: EntryFilter?,
) {
    fun toModule(rootDirectory: File) = Module(
        rootDirectory = rootDirectory,
        name = name,
        sampleDirectory = sampleDirectory.toFile(),
        entries = entries,
        currentIndex = currentIndex,
        rawFilePath = rawFilePath?.toFile(),
        entryFilter = entryFilter,
    )
}

fun Module.toJs(project: Project) = JsModule(
    name = name,
    sampleDirectory = getSampleDirectory(project).absolutePath,
    entries = entries,
    currentIndex = currentIndex,
    rawFilePath = getRawFile(project)?.absolutePath,
    entryFilter = entryFilter,
)
