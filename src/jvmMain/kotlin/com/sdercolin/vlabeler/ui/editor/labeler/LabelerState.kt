package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LabelerState(canvasResolution: Int) {

    var canvasResolution: Int by mutableStateOf(canvasResolution)
        private set

    fun changeResolution(resolution: Int) {
        canvasResolution = resolution
    }
}
