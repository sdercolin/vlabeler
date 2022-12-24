package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Entry

@Immutable
data class IndexedEntry(
    val entry: Entry,
    val index: Int,
) {
    val sample get() = entry.sample
    val name get() = entry.name
    val start get() = entry.start
    val end get() = entry.end
    val points get() = entry.points
    val extras get() = entry.extras

    fun edit(entry: Entry) = copy(entry = entry)
}
