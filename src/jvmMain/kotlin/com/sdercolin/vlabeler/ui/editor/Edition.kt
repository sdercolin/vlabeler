package com.sdercolin.vlabeler.ui.editor

import com.sdercolin.vlabeler.model.Entry

/**
 * Represents an edition of an entry.
 *
 * @property index The index of the entry.
 * @property newValue The new value of the entry.
 * @property fieldNames The names of the fields that were edited.
 * @property method The method used to edit the entry.
 */
data class Edition(
    val index: Int,
    val newValue: Entry,
    val fieldNames: List<String>,
    val method: Method,
) {
    fun toIndexedEntry() = IndexedEntry(newValue, index)

    enum class Method {
        Dragging,
        SetWithCursor,
    }
}
