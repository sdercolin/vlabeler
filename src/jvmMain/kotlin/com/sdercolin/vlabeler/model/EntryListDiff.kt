package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.EntryListDiffItem.Add
import com.sdercolin.vlabeler.model.EntryListDiffItem.Edit
import com.sdercolin.vlabeler.model.EntryListDiffItem.Remove
import com.sdercolin.vlabeler.model.EntryListDiffItem.Unchanged
import kotlinx.serialization.Serializable

/**
 * Represents a change in an entry list.
 */
sealed class EntryListDiffItem {
    data class Add(val newIndex: Int, val new: Entry) : EntryListDiffItem()
    data class Remove(val oldIndex: Int, val old: Entry) : EntryListDiffItem()
    data class Edit(val oldIndex: Int, val old: Entry, val newIndex: Int, val new: Entry) : EntryListDiffItem()
    data class Unchanged(val oldIndex: Int, val old: Entry, val newIndex: Int, val new: Entry) : EntryListDiffItem() {
        constructor(oldIndex: Int, newIndex: Int, entry: Entry) : this(oldIndex, entry, newIndex, entry)
    }
}

/**
 * Represents the difference between two entry lists.
 */
data class EntryListDiff(val items: List<EntryListDiffItem>)

/**
 * Configuration for the weights of different properties in the similarity score calculation.
 *
 * @param name Weight for the [Entry.name] property.
 * @param sample Weight for the [Entry.sample] property.
 * @param start Weight for the [Entry.start] property.
 * @param end Weight for the [Entry.end] property.
 * @param points Weights for the [Entry.points] property. The size of the list should match the size of the points list.
 *     If this array is empty or shorter than the points list, the missing weights will be considered as 0.
 * @param extras Weights for the [Entry.extras] property. The size of the list should match the size of the extras list.
 *     If this array is empty or shorter than the points list, the missing weights will be considered as 0.
 * @param tag Weight for the [EntryNotes.tag] property.
 * @param threshold The minimum similarity score for two entries to be considered as the same entry.
 */
@Serializable
@Immutable
data class EntrySimilarityWeights(
    val name: Double = 0.5,
    val sample: Double = 0.3,
    val start: Double = 0.1,
    val end: Double = 0.1,
    val points: List<Double> = emptyList(),
    val extras: List<Double> = emptyList(),
    val tag: Double = 0.0,
    val threshold: Double = 0.75,
)

private fun getSimilarityScore(
    entry1: Entry,
    entry2: Entry,
    weights: EntrySimilarityWeights,
): Double {
    var totalWeight = 0.0
    var score = 0.0

    fun handle(property: (Entry) -> Any, weight: Double) {
        val value1 = property(entry1)
        val value2 = property(entry2)
        if (value1 == value2) {
            score += weight
        }
        totalWeight += weight
    }

    handle(Entry::name, weights.name)
    handle(Entry::sample, weights.sample)
    handle(Entry::start, weights.start)
    handle(Entry::end, weights.end)
    weights.points.forEachIndexed { index, weight ->
        val value1 = entry1.points.getOrNull(index) ?: return@forEachIndexed
        val value2 = entry2.points.getOrNull(index) ?: return@forEachIndexed
        val valueScore = if (value1 == value2) weight else 0.0
        if (value1 == value2) {
            score += valueScore
        }
        totalWeight += weight
    }
    weights.extras.forEachIndexed { index, weight ->
        val extra1 = entry1.extras.getOrNull(index) ?: return@forEachIndexed
        val extra2 = entry2.extras.getOrNull(index) ?: return@forEachIndexed
        if (extra1 == extra2) {
            score += weight
        }
        totalWeight += weight
    }
    handle({ it.notes.tag }, weights.tag)
    return score / totalWeight
}

/**
 * Computes the difference between two entry lists.
 */
fun computeEntryListDiff(
    list1: List<Entry>,
    list2: List<Entry>,
    weights: EntrySimilarityWeights = EntrySimilarityWeights(),
): EntryListDiff {
    val diffs = mutableListOf<EntryListDiffItem>()
    val matched = mutableSetOf<IndexedValue<Entry>>()

    // Mapping based on highest similarity score
    val mapping = list1.withIndex().associateWith { obj1 ->
        list2.withIndex().mapNotNull { obj2 ->
            val score = getSimilarityScore(obj1.value, obj2.value, weights)
            if (score >= weights.threshold) obj2 to score else null
        }.maxByOrNull { it.second }?.first
    }
    val unchangedMap = mapping.mapNotNull { (obj1, obj2) -> if (obj2 != null) obj1.index to obj2.index else null }
        .toMap().toMutableMap()

    // Determine edits and removes
    mapping.forEach { (obj1, obj2) ->
        when {
            obj2 == null -> diffs.add(Remove(obj1.index, obj1.value))
            getSimilarityScore(obj1.value, obj2.value, weights) < 1.0 -> {
                diffs.add(Edit(obj1.index, obj1.value, obj2.index, obj2.value))
                matched.add(obj2)
                unchangedMap.remove(obj1.index)
            }
            else -> matched.add(obj2)
        }
    }

    // Determine adds
    list2.withIndex().filterNot { it in matched }.forEach { diffs.add(Add(it.index, it.value)) }

    // Add unchanged items
    unchangedMap.forEach { (oldIndex, newIndex) ->
        diffs.add(Unchanged(oldIndex, list1[oldIndex], newIndex, list2[newIndex]))
    }

    val sortedDiffs = diffs.sortedWith(
        compareBy<EntryListDiffItem> { diff ->
            // Primary sorting by the relevant index
            when (diff) {
                is Add -> diff.newIndex
                is Remove -> diff.oldIndex
                is Edit -> diff.newIndex
                is Unchanged -> diff.newIndex
            }
        }.thenComparing { diff ->
            // Secondary sorting to prioritize removes over adds/edits at the same index
            when (diff) {
                is Remove -> 0
                is Edit -> 1
                is Add -> 2
                is Unchanged -> 3
            }
        },
    )
    return EntryListDiff(sortedDiffs)
}
