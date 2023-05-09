package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPage
import com.sdercolin.vlabeler.util.savedMutableStateOf

interface AppViewState {
    var isMarkerDisplayed: Boolean
    var isPropertyViewDisplayed: Boolean
    var isEntryListPinned: Boolean
    var pinnedEntryListSplitPanePositionLocked: Boolean
    var isToolboxDisplayed: Boolean
    var isTimescaleBarDisplayed: Boolean
    var lastViewedPreferencesPage: PreferencesPage?
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

    override var pinnedEntryListSplitPanePositionLocked: Boolean
        by savedMutableStateOf(appRecord.value.pinnedEntryListSplitPanePositionLocked) {
            appRecord.update { copy(pinnedEntryListSplitPanePositionLocked = it) }
        }

    override var isToolboxDisplayed: Boolean
        by savedMutableStateOf(appRecord.value.isToolboxDisplayed) {
            appRecord.update { copy(isToolboxDisplayed = it) }
        }

    override var isTimescaleBarDisplayed: Boolean
        by savedMutableStateOf(appRecord.value.isTimescaleBarDisplayed) {
            appRecord.update { copy(isTimescaleBarDisplayed = it) }
        }

    override var lastViewedPreferencesPage: PreferencesPage? by mutableStateOf(null)
}
