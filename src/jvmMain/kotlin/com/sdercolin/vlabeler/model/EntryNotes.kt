package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Meta information of an entry which are only used in vLabeler.
 *
 * @property done Whether the entry has been edited.
 * @property star Whether the entry is starred.
 * @property tag Tag of the entry.
 */
@Serializable
@Immutable
data class EntryNotes(
    val done: Boolean = false,
    val star: Boolean = false,
    val tag: String = "",
)
