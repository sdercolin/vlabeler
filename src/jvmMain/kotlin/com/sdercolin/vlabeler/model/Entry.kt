package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class Entry(
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
    val extra: List<String>
) {
    companion object {
        fun fromDefaultValues(name: String, defaultValues: List<Float>) = Entry(
            name = name,
            start = defaultValues.first(),
            end = defaultValues.last(),
            points = defaultValues.drop(1).dropLast(1),
            extra = listOf()
        )
    }
}
