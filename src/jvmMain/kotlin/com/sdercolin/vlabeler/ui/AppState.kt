package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs

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
    val shouldExit: Boolean = false,
) {

    val hasProject get() = project != null
    fun openProject(project: Project) = AppState(project = project)
    private fun closeProject() = AppState()
    fun editProject(editor: Project.() -> Project) = copy(project = project!!.editor())

    fun configureNewProject() = copy(isConfiguringNewProject = true)
    fun stopConfiguringNewProject() = copy(isConfiguringNewProject = false)

    fun requestOpenProject() = if (hasUnsavedChanges) askIfSaveBeforeOpenProject() else openOpenProjectDialog()
    private fun askIfSaveBeforeOpenProject() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsOpening)
    fun openOpenProjectDialog() = copy(isShowingOpenProjectDialog = true)
    fun closeOpenProjectDialog() = copy(isShowingOpenProjectDialog = false)

    fun openSaveAsProjectDialog() = copy(isShowingSaveAsProjectDialog = true)
    fun closeSaveAsProjectDialog() = copy(isShowingSaveAsProjectDialog = false)

    fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
    private fun askIfSaveBeforeExport() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsExporting)
    private fun openExportDialog() = copy(isShowingExportDialog = true)
    fun closeExportDialog() = copy(isShowingExportDialog = false)

    fun requestCloseProject() = if (hasUnsavedChanges) askIfSaveBeforeCloseProject() else closeProject()
    private fun askIfSaveBeforeCloseProject() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsClosing)
    val hasUnsavedChanges get() = projectWriteStatus == ProjectWriteStatus.Changed || hasEditedEntry

    fun requestSave(pendingAction: PendingActionAfterSaved? = null) =
        copy(
            projectWriteStatus = ProjectWriteStatus.UpdateRequested,
            pendingActionAfterSaved = pendingAction
        )

    fun takeAskIfSaveResult(result: AskIfSaveDialogResult) =
        if (result.save) {
            requestSave(result.actionAfterSaved)
        } else consumePendingActionAfterSaved(result.actionAfterSaved)

    fun saved() = copy(projectWriteStatus = ProjectWriteStatus.Updated)
        .consumePendingActionAfterSaved(pendingActionAfterSaved)

    private fun consumePendingActionAfterSaved(action: PendingActionAfterSaved?) = when (action) {
        PendingActionAfterSaved.Open -> openOpenProjectDialog()
        PendingActionAfterSaved.Export -> openExportDialog()
        PendingActionAfterSaved.Close -> closeProject()
        PendingActionAfterSaved.Exit -> exit()
        null -> this
    }

    fun openEmbeddedDialog(args: EmbeddedDialogArgs) = copy(embeddedDialog = args)
    fun closeEmbeddedDialog() = copy(embeddedDialog = null)

    fun openJumpToEntryDialog() = copy(embeddedDialog = JumpToEntryDialogArgs(project!!))

    fun jumpToEntry(sampleName: String, entryIndex: Int) = copy(
        project = project!!.copy(
            currentSampleName = sampleName,
            currentEntryIndex = entryIndex
        )
    )

    fun projectContentChanged() = copy(projectWriteStatus = ProjectWriteStatus.Changed)
    fun projectPathChanged() = copy(projectWriteStatus = ProjectWriteStatus.Updated)

    fun localEntryEdited() = copy(hasEditedEntry = true)
    fun editedEntryMerged() = copy(hasEditedEntry = false)

    fun requestExit() = if (hasUnsavedChanges) askIfSaveBeforeExit() else exit()
    private fun askIfSaveBeforeExit() = copy(embeddedDialog = AskIfSaveDialogPurpose.IsExiting)
    private fun exit() = copy(shouldExit = true)

    val isEditorActive
        get() = project != null &&
            !isConfiguringNewProject &&
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
        Open,
        Export,
        Close,
        Exit
    }
}
