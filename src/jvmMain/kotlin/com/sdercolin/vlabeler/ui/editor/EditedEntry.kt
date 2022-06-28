package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Entry

@Immutable
data class EditedEntry(
    val entry: Entry,
    val sampleName: String,
    val index: Int
) {
    fun edit(entry: Entry) = copy(entry = entry)
}
