package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Entry(
    /**
     * File name of the sample wav file without extension
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
     * Label end time (milliseconds)
     * Minus or zero value represents a relative value to the sample file's end.
     * Will be converted to positive value when edited
     */
    val end: Float,
    /**
     * Other points (milliseconds) defined by [LabelerConf.fields]
     */
    val points: List<Float>,
    /**
     * Extra data as [String] defined by [LabelerConf.extraFieldNames]
     */
    val extras: List<String>,
    /**
     * Other properties of an entry which are only used in vLabeler
     */
    val meta: EntryMetaData = EntryMetaData(),
) {

    fun starToggled() = copy(meta = meta.copy(star = !meta.star))
    fun doneToggled() = copy(meta = meta.copy(done = !meta.done))
    fun done() = copy(meta = meta.copy(done = true))

    companion object {
        fun fromDefaultValues(sample: String, name: String, labelerConf: LabelerConf) =
            Entry(
                sample = sample,
                name = name,
                start = labelerConf.defaultValues.first(),
                end = labelerConf.defaultValues.last(),
                points = labelerConf.defaultValues.drop(1).dropLast(1),
                extras = labelerConf.defaultExtras,
            )
    }
}
