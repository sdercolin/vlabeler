package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.filter.EntryFilter

class EntryListFilterState {
    var filter: EntryFilter by mutableStateOf(EntryFilter())
        private set
    var linked: Boolean by mutableStateOf(false)

    fun editFilter(editor: EntryFilter.() -> EntryFilter) {
        filter = editor.invoke(filter)
    }
}
