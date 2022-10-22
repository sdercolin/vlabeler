package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
@Immutable
data class Module(
    val name: String,
    val sampleDirectory: String,
    val entries: List<Entry>,
    val currentIndex: Int,
    val rawFilePath: String? = null,
    val entryFilter: EntryFilter? = null,
) {
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

    val currentSampleName: String
        get() = currentEntry.sample

    val currentSampleFile: File
        get() = getSampleFile(currentSampleName)

    fun getSampleFile(sampleName: String): File {
        return File(sampleDirectory, sampleName)
    }

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
            .filter { entry -> currentEntryGroup.none { it.index == entry.index + 1 } && entry.value.end <= 0f }
            .map {
                val end = sampleInfo.lengthMillis + it.value.end
                it.copy(value = it.value.copy(end = end))
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

    private fun updateEntry(editedEntry: IndexedEntry, labelerConf: LabelerConf) =
        updateEntries(listOf(editedEntry), labelerConf)

    fun markEntriesAsDone(editedIndexes: Set<Int>): Module {
        val entries = entries.toMutableList()
        editedIndexes.forEach {
            entries[it] = entries[it].done()
        }
        return copy(entries = entries)
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
            require(it.extras.size == labelerConf.extraFieldNames.size) {
                "Extra size doesn't match in entry: $it. Required extra size = ${labelerConf.extraFieldNames.size}"
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
