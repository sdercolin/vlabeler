package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs

@Immutable
data class AppState(
    val project: Project? = null,
    val isConfiguringNewProject: Boolean = false,
    val isShowingOpenProjectDialog: Boolean = false,
    val isShowingSaveAsProjectDialog: Boolean = false,
    val isShowingExportDialog: Boolean = false,
    val pendingActionAfterSaved: PendingActionAfterSaved? = null,
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

    val hasProject get() = project != null
    fun openProject(project: Project) = AppState(project = project)
    fun closeProject() = copy(project = null)
    fun editProject(editor: Project.() -> Project) = copy(project = editor(project!!))

    fun configureNewProject() = copy(isConfiguringNewProject = true)
    fun stopConfiguringNewProject() = copy(isConfiguringNewProject = false)

    val hasUnsavedChanges get() = projectWriteStatus == ProjectWriteStatus.Changed || hasEditedEntry

    val isEditorActive
        get() = !isConfiguringNewProject &&
            !isShowingOpenProjectDialog &&
            !isShowingSaveAsProjectDialog &&
            !isShowingExportDialog &&
            embeddedDialog == null

    fun requestSave(pendingAction: PendingActionAfterSaved? = null) =
        copy(
            projectWriteStatus = ProjectWriteStatus.UpdateRequested,
            pendingActionAfterSaved = pendingAction
        )

    fun requestExport() = if (hasUnsavedChanges) {
        copy(embeddedDialog = AskIfSaveDialogPurpose.IsExporting)
    } else {
        copy(isShowingExportDialog = true)
    }

    fun requestClose() = if (hasUnsavedChanges) askSaveBeforeClose() else closeProject()
    fun askSaveBeforeClose() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsClosing)
    fun saved() = copy(projectWriteStatus = ProjectWriteStatus.Updated).consumePendingActionAfterSaved()

    private fun consumePendingActionAfterSaved() = when (pendingActionAfterSaved) {
        PendingActionAfterSaved.Export -> copy(isShowingExportDialog = true)
        PendingActionAfterSaved.Close -> closeProject()
        null -> this
    }

    enum class ProjectWriteStatus {
        Updated,
        Changed,
        UpdateRequested
    }

    enum class PendingActionAfterSaved {
        Export,
        Close
    }
}
