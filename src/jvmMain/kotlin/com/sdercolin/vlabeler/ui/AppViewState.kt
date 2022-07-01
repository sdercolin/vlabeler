package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.cached

class AppViewState(appRecord: AppRecordStore) {
    var isMarkerDisplayed: Boolean by mutableStateOf(true)

    var isPropertyViewDisplayed by appRecord.cached(
        selector = { it.isPropertyViewDisplayed },
        updater = { base, value -> base.copy(isPropertyViewDisplayed = value) }
    )

    var isEntryListPinned: Boolean by appRecord.cached(
        selector = { it.isEntryListPinned },
        updater = { base, value -> base.copy(isEntryListPinned = value) }
    )
}
