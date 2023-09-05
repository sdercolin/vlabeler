package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerScissorsState(
    val position: Float? = null,
    val locked: Boolean = false,
    val disabled: Boolean = false,
)
