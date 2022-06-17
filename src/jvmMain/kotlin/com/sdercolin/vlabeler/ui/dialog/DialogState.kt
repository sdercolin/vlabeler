package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Immutable

@Immutable
data class DialogState(
    val openProject: Boolean = false,
    val embedded: EmbeddedDialogArgs? = null
)
