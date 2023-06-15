package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerPlaybackState(
    val position: Float? = null,
    val isDragging: Boolean = false,
)
