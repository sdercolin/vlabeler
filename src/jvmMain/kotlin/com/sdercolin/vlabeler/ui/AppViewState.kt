package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.savedMutableStateOf

interface AppViewState {
    var isMarkerDisplayed: Boolean
    var isPropertyViewDisplayed: Boolean
    var isEntryListPinned: Boolean
    var isToolboxDisplayed: Boolean
}

class AppViewStateImpl(appRecord: AppRecordStore) : AppViewState {
    override var isMarkerDisplayed: Boolean by mutableStateOf(true)

    override var isPropertyViewDisplayed: Boolean
        by savedMutableStateOf(appRecord.stateFlow.value.isPropertyViewDisplayed) {
            appRecord.update { copy(isPropertyViewDisplayed = it) }
        }

    override var isEntryListPinned: Boolean
        by savedMutableStateOf(appRecord.stateFlow.value.isEntryListPinned) {
            appRecord.update { copy(isEntryListPinned = it) }
        }

    override var isToolboxDisplayed: Boolean
        by savedMutableStateOf(appRecord.stateFlow.value.isToolboxDisplayed) {
            appRecord.update { copy(isToolboxDisplayed = it) }
        }
}
