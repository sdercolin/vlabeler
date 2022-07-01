package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.savedMutableStateOf

interface AppViewState {
    var isMarkerDisplayed: Boolean
    var isPropertyViewDisplayed: Boolean
    var isEntryListPinned: Boolean
}

class AppViewStateImpl(appRecord: AppRecordStore) : AppViewState {
    override var isMarkerDisplayed: Boolean by mutableStateOf(true)

    override var isPropertyViewDisplayed: Boolean
        by savedMutableStateOf(appRecord.value.isPropertyViewDisplayed) {
            appRecord.update { copy(isPropertyViewDisplayed = it) }
        }

    override var isEntryListPinned: Boolean
        by savedMutableStateOf(appRecord.value.isEntryListPinned) {
            appRecord.update { copy(isEntryListPinned = it) }
        }
}
