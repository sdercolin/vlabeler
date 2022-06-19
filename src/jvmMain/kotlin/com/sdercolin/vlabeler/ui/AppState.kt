package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs

@Immutable
data class AppState(
    val isConfiguringNewProject: Boolean = false,
    val isShowingOpenProjectDialog: Boolean = false,
    val embeddedDialog: EmbeddedDialogArgs? = null,
    /**
     * Describes the update status between [Project] state and project file
     */
    val projectWriteStatus: ProjectWriteStatus = ProjectWriteStatus.Updated,
    /**
     * True if the editor has not saved its changes on the current entry to the [Project] state
     */
    val hasEditedEntry: Boolean = false,
) {

    val isSaveEnabled get() = projectWriteStatus == ProjectWriteStatus.Changed || hasEditedEntry

    enum class ProjectWriteStatus {
        Updated,
        Changed,
        UpdateRequested
    }
}
