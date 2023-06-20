package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerPlaybackState(
    val position: Float? = null,
    val draggingStartPosition: Float? = null,
) {
    fun startDragging(position: Float) = copy(draggingStartPosition = position)
    fun finishDragging() = copy(draggingStartPosition = null)
}
