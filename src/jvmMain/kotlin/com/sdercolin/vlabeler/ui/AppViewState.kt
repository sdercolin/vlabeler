package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.cached

interface AppViewState {
    var isMarkerDisplayed: Boolean
    var isPropertyViewDisplayed: Boolean
    var isEntryListPinned: Boolean
}

class AppViewStateImpl(appRecord: AppRecordStore) : AppViewState {
    override var isMarkerDisplayed: Boolean by mutableStateOf(true)

    override var isPropertyViewDisplayed by appRecord.cached(
        selector = { it.isPropertyViewDisplayed },
        updater = { base, value -> base.copy(isPropertyViewDisplayed = value) }
    )

    override var isEntryListPinned: Boolean by appRecord.cached(
        selector = { it.isEntryListPinned },
        updater = { base, value -> base.copy(isEntryListPinned = value) }
    )
}
