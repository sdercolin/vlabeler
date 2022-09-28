@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Entry.Companion.EntrySerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder

@Serializable(EntrySerializer::class)
@Immutable
data class Entry(
    /**
     * Sample file name without extension
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
    val notes: EntryNotes = EntryNotes(),
) {

    fun starToggled() = copy(notes = notes.copy(star = !notes.star))
    fun doneToggled() = copy(notes = notes.copy(done = !notes.done))
    fun done() = copy(notes = notes.copy(done = true))
    fun tagEdited(tag: String) = copy(notes = notes.copy(tag = tag))

    @Serializable
    data class EntryDeserializationContainer(
        val sample: String,
        val name: String,
        val start: Float,
        val end: Float,
        val points: List<Float>,
        val extras: List<String>,
        val notes: EntryNotes? = null,
        // for backward compatibility
        val meta: EntryNotes? = null,
    ) {

        fun toEntry() = Entry(
            sample = sample,
            name = name,
            start = start,
            end = end,
            points = points,
            extras = extras,
            notes = notes ?: meta ?: EntryNotes(),
        )
    }

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

        @Serializer(Entry::class)
        object EntrySerializer : KSerializer<Entry> {
            override fun deserialize(decoder: Decoder): Entry {
                require(decoder is JsonDecoder)
                val element = decoder.decodeSerializableValue(EntryDeserializationContainer.serializer())
                return element.toEntry()
            }
        }
    }
}
