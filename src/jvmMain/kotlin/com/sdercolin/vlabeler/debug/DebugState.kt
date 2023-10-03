package com.sdercolin.vlabeler.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DebugState {
    var isShowingChunkBorder: Boolean by mutableStateOf(false)
    var printMemoryUsage: Boolean by mutableStateOf(false)
    var forceUseCustomFileDialog: Boolean by mutableStateOf(false)
    var isShowingFontPreviewDialog: Boolean by mutableStateOf(false)
}
