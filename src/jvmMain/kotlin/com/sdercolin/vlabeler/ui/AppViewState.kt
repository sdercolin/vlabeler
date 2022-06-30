package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppViewState(private val appRecord: AppRecordStore) {
    var isMarkerDisplayed: Boolean by mutableStateOf(true)

    private val isPropertyViewDisplayedState = mutableStateOf(appRecord.value.isPropertyViewDisplayed)
    var isPropertyViewDisplayed: Boolean
        get() = isPropertyViewDisplayedState.value
        set(value) {
            isPropertyViewDisplayedState.value = value
            appRecord.update { copy(isPropertyViewDisplayed = value) }
        }

    private val isEntryListPinnedState = mutableStateOf(appRecord.value.isEntryListPinned)
    var isEntryListPinned: Boolean
        get() = isEntryListPinnedState.value
        set(value) {
            isEntryListPinnedState.value = value
            appRecord.update { copy(isEntryListPinned = value) }
        }
}
