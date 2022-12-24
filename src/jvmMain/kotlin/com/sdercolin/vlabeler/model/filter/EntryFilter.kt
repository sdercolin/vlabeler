package com.sdercolin.vlabeler.model.filter

import com.sdercolin.vlabeler.model.Entry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class EntryFilter(
    val searchText: String = "",
    val star: Boolean? = null,
    val done: Boolean? = null,
) {
    fun isEmpty(): Boolean {
        return searchText.isEmpty() && star == null && done == null
    }

    @Transient
    private var searchAny: String? = null

    @Transient
    private var searchName: String? = null

    @Transient
    private var searchSample: String? = null

    @Transient
    private var searchTag: String? = null

    init {

        fun String.getValue() = split(":")[1]
        fun String.trimValue() = if (startsWith("\"") && endsWith("\"") && length > 1) {
            substring(1, length - 1)
        } else {
            this
        }

        val sections = searchText.split(";")
        for (section in sections) {
            val trimmed = section.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("name:") -> searchName = trimmed.getValue().trimValue()
                trimmed.startsWith("sample:") -> searchSample = trimmed.getValue().trimValue()
                trimmed.startsWith("tag:") -> searchTag = trimmed.getValue().trimValue()
                else -> searchAny = trimmed.trimValue()
            }
        }
    }

    fun matches(entry: Entry): Boolean {
        searchAny?.let {
            if (!entry.name.contains(it) &&
                !entry.sampleNameWithoutExtension.contains(it) &&
                !entry.notes.tag.contains(it)
            ) {
                return false
            }
        }
        searchName?.let {
            if (!entry.name.contains(it)) {
                return false
            }
        }
        searchSample?.let {
            if (!entry.sampleNameWithoutExtension.contains(it)) {
                return false
            }
        }
        searchTag?.let {
            if (!entry.notes.tag.contains(it)) {
                return false
            }
        }
        if (star != null && entry.notes.star != star) {
            return false
        }
        if (done != null && entry.notes.done != done) {
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
