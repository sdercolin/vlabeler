package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs

@Immutable
data class AppState(
    val isConfiguringNewProject: Boolean = false,
    val isShowingOpenProjectDialog: Boolean = false,
    val embeddedDialog: EmbeddedDialogArgs? = null
)
