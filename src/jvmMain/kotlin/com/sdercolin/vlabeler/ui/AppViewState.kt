package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppRecord

class AppViewState(appRecord: MutableState<AppRecord>) {
    var isMarkerDisplayed: Boolean by mutableStateOf(appRecord.value.isMarkerDisplayed)
}
