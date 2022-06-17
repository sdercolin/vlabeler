package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.runtime.Immutable

@Immutable
data class DialogState(
    val openFile: Boolean = false,
    val embedded: EmbeddedDialogArgs? = null
)
