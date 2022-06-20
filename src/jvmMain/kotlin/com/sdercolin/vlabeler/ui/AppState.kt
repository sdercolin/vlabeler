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

    fun openOpenProjectDialog() = copy(isShowingOpenProjectDialog = true)
    fun closeOpenProjectDialog() = copy(isShowingOpenProjectDialog = false)

    fun openSaveAsProjectDialog() = copy(isShowingSaveAsProjectDialog = true)
    fun closeSaveAsProjectDialog() = copy(isShowingSaveAsProjectDialog = false)

    fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
    private fun askIfSaveBeforeExport() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsExporting)
    fun openExportDialog() = copy(isShowingExportDialog = true)
    fun closeExportDialog() = copy(isShowingExportDialog = false)

    fun requestClose() = if (hasUnsavedChanges) askIfSaveBeforeClose() else closeProject()
    private fun askIfSaveBeforeClose() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsClosing)
    val hasUnsavedChanges get() = projectWriteStatus == ProjectWriteStatus.Changed || hasEditedEntry

    fun requestSave(pendingAction: PendingActionAfterSaved? = null) =
        copy(
            projectWriteStatus = ProjectWriteStatus.UpdateRequested,
            pendingActionAfterSaved = pendingAction
        )

    fun saved() = copy(projectWriteStatus = ProjectWriteStatus.Updated).consumePendingActionAfterSaved()

    private fun consumePendingActionAfterSaved() = when (pendingActionAfterSaved) {
        PendingActionAfterSaved.Export -> copy(isShowingExportDialog = true)
        PendingActionAfterSaved.Close -> closeProject()
        null -> this
    }

    fun openEmbeddedDialog(args: EmbeddedDialogArgs) = copy(embeddedDialog = args)
    fun closeEmbeddedDialog() = copy(embeddedDialog = null)

    val isEditorActive
        get() = !isConfiguringNewProject &&
            !isShowingOpenProjectDialog &&
            !isShowingSaveAsProjectDialog &&
            !isShowingExportDialog &&
            embeddedDialog == null

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
