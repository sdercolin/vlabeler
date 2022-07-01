package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.util.backed

class AppViewState(appRecord: AppRecordStore) {
    var isMarkerDisplayed: Boolean by mutableStateOf(true)

    var isPropertyViewDisplayed by appRecord.backed(
        valueSelector = { it.isPropertyViewDisplayed },
        valueUpdater = { base, value -> base.copy(isPropertyViewDisplayed = value) }
    )

    var isEntryListPinned: Boolean by appRecord.backed(
        valueSelector = { it.isEntryListPinned },
        valueUpdater = { base, value -> base.copy(isEntryListPinned = value) }
    )
}
