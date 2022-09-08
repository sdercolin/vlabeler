package com.sdercolin.vlabeler.model.filter

import com.sdercolin.vlabeler.model.Entry
import kotlinx.serialization.Serializable

@Serializable
data class EntryFilter(
    val searchText: String = "",
) {
    fun matches(entry: Entry): Boolean {
        return searchText.isEmpty() || entry.name.contains(searchText, true)
    }

    fun validated(): EntryFilter? {
        if (searchText.isEmpty()) return null
        return this
    }
}
