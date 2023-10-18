package com.sdercolin.vlabeler.model.filter

import com.sdercolin.vlabeler.model.Entry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A serializable data class to represent a filter for [Entry].
 *
 * @property searchText The combined text prompt for searching. e.g. `"any";name:"name";sample:"sample";tag:"tag"`
 * @property star The star filter. If null, no filter is applied.
 * @property done The done filter. If null, no filter is applied.
 */
@Serializable
data class EntryFilter(
    val searchText: String = "",
    val star: Boolean? = null,
    val done: Boolean? = null,
) {
    fun isEmpty(): Boolean {
        return searchText.isEmpty() && star == null && done == null
    }

    fun parse(): Args {
        var name: String? = null
        var sample: String? = null
        var tag: String? = null
        var any: String? = null
        fun String.getValue() = split(":")[1]
        fun String.trimValue() = if (startsWith("\"") && endsWith("\"") && length > 1) {
            substring(1, length - 1)
        } else {
            this
        }.ifEmpty { null }

        val sections = searchText.split(";")
        for (section in sections) {
            val trimmed = section.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("name:") -> name = trimmed.getValue().trimValue()
                trimmed.startsWith("sample:") -> sample = trimmed.getValue().trimValue()
                trimmed.startsWith("tag:") -> tag = trimmed.getValue().trimValue()
                else -> any = trimmed.trimValue()
            }
        }
        return Args(any, name, sample, tag, star, done)
    }

    data class Args(
        val any: String? = null,
        val name: String? = null,
        val sample: String? = null,
        val tag: String? = null,
        val star: Boolean? = null,
        val done: Boolean? = null,
    ) {
        fun toEntryFilter(): EntryFilter {
            val text = buildString {
                if (any.isNullOrEmpty().not()) {
                    append(any)
                }
                if (name.isNullOrEmpty().not()) {
                    if (isNotEmpty()) append(";")
                    append("name:$name")
                }
                if (sample.isNullOrEmpty().not()) {
                    if (isNotEmpty()) append(";")
                    append("sample:$sample")
                }
                if (tag.isNullOrEmpty().not()) {
                    if (isNotEmpty()) append(";")
                    append("tag:$tag")
                }
            }
            return EntryFilter(text, star, done)
        }
    }

    @Transient
    private val args = parse()

    fun matches(entry: Entry): Boolean {
        args.any?.let {
            if (!entry.name.contains(it) &&
                !entry.sampleNameWithoutExtension.contains(it) &&
                !entry.notes.tag.contains(it)
            ) {
                return false
            }
        }
        args.name?.let {
            if (!entry.name.contains(it)) {
                return false
            }
        }
        args.sample?.let {
            if (!entry.sampleNameWithoutExtension.contains(it)) {
                return false
            }
        }
        args.tag?.let {
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
