package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.SampleInfo

@Immutable
sealed class CanvasState {

    @Immutable
    object Loading : CanvasState()

    @Immutable
    object Error : CanvasState()

    @Immutable
    data class Loaded(
        val params: CanvasParams,
        val sampleInfo: SampleInfo,
    ) : CanvasState()
}
