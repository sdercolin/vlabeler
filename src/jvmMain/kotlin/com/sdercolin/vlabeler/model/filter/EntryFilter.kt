package com.sdercolin.vlabeler.model.filter

import com.sdercolin.vlabeler.model.Entry
import kotlinx.serialization.Serializable

@Serializable
data class EntryFilter(
    val searchText: String = "",
    val star: Boolean? = null,
    val done: Boolean? = null,
) {
    fun matches(entry: Entry): Boolean {
        if (searchText.isNotEmpty() && !entry.name.contains(searchText, true)) {
            return false
        }
        if (star != null && entry.meta.star != star) {
            return false
        }
        if (done != null && entry.meta.done != done) {
            return false
        }
        return true
    }

    fun starNexted() = copy(
        star = when (star) {
            null -> true
            true -> false
            false -> null
        },
    )

    fun doneNexted() = copy(
        done = when (done) {
            null -> true
            true -> false
            false -> null
        },
    )
}
