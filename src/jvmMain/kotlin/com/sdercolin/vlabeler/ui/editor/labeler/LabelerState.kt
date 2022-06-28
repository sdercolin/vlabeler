package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf

class LabelerState(canvasResolution: Int) {

    var canvasResolution: Int by mutableStateOf(canvasResolution)
        private set

    fun changeResolution(resolution: Int) {
        canvasResolution = resolution
    }
}

@Composable
fun rememberLabelerState(appConf: AppConf) = remember {
    LabelerState(appConf.painter.canvasResolution.default)
}