package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A serializable data to represent a label entry.
 *
 * @property sample Sample file name with extension.
 * @property name Name or alias of the entry.
 * @property start Label start time in milliseconds.
 * @property end Label end time in milliseconds. Minus or zero value represents a relative value to the sample file's
 *     end, which will be converted to positive value the sample is firstly loaded.
 * @property points Times in milliseconds for other points defined by [LabelerConf.fields].
 * @property extras Extra data as [String] defined by [LabelerConf.extraFields]. If [LabelerConf.ExtraField.isOptional]
 *     is `true`, the corresponding value could be `null`.
 * @property notes Other properties of an entry which are only used in vLabeler.
 * @property needSync Whether the entry need to be updated with the sample file. Especially when `end` is `0`, we don't
 *     know if it's the actual start of the sample file or a relative value to the end, which needs to be converted to
 *     an absolute value with the sample file's length.
 */
@Serializable
@Immutable
data class Entry(
    /**
     * Sample file name with extension
     */
    val sample: String,
    /**
     * Name or Alias of the entry
     */
    val name: String,
    /**
     * Label start time (milliseconds)
     */
    val start: Float,
    /**
     * Label end time (milliseconds) Minus or zero value represents a relative value to the sample file's end. Will be
     * converted to positive value when edited
     */
    val end: Float,
    /**
     * Other points (milliseconds) defined by [LabelerConf.fields]
     */
    val points: List<Float>,
    /**
     * Extra data as [String] defined by [LabelerConf.extraFields]
     */
    val extras: List<String?>,
    /**
     * Other properties of an entry which are only used in vLabeler
     */
    val notes: EntryNotes = EntryNotes(),
    /**
     * Whether the entry need to be updated with the sample file. Especially when `end` is `0`, we don't know if it's
     * the actual start of the sample file or a relative value to the end, which means we need to sync it with the
     * sample file
     */
    val needSync: Boolean = false,
) {

    val sampleNameWithoutExtension: String
        get() = sample.substringBeforeLast('.')

    fun starToggled() = copy(notes = notes.copy(star = !notes.star))
    fun doneToggled() = copy(notes = notes.copy(done = !notes.done))
    fun done() = copy(notes = notes.copy(done = true))
    fun tagEdited(tag: String) = copy(notes = notes.copy(tag = tag))

    /**
     * For entries created by older versions of labelers, `needSync` may not be set correctly.
     */
    val needSyncCompatibly get() = (needSync && end == 0f) || end < 0

    companion object {
        fun fromDefaultValues(sample: String, labelerConf: LabelerConf) =
            Entry(
                sample = sample,
                name = labelerConf.defaultEntryName ?: sample.substringBeforeLast('.'),
                start = labelerConf.defaultValues.first(),
                end = labelerConf.defaultValues.last(),
                points = labelerConf.defaultValues.drop(1).dropLast(1),
                extras = labelerConf.extraFields.map { it.default },
                needSync = true,
            )
    }
}
