package com.sdercolin.vlabeler.ui.dialog.sample

import com.sdercolin.vlabeler.ui.editor.IndexedEntry

sealed class SampleListDialogItem(open val name: String) {

    sealed class Sample(override val name: String) : SampleListDialogItem(name) {
        fun isSelected(selectedName: String?) = name == selectedName
    }

    data class IncludedSample(
        override val name: String,
        val valid: Boolean,
        val entryCount: Int,
    ) : Sample(name)

    data class ExcludedSample(override val name: String) : Sample(name)

    data class Entry(override val name: String, val entry: IndexedEntry) : SampleListDialogItem(name) {
        fun isSelected(selectedIndex: Int?) = entry.index == selectedIndex
    }
}
