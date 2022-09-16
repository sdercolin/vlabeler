package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class EntryNotes(
    val done: Boolean = false,
    val star: Boolean = false,
    val tag: String = "",
)
