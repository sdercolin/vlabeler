package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs

@Immutable
data class AppState(
    val project: Project? = null,
    val history: ProjectHistory = ProjectHistory(),
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
    val shouldExit: Boolean = false,
) {

    val hasProject get() = project != null
    fun openProject(project: Project) = AppState(project = project, history = history.new(project))
    private fun closeProject() = AppState()

    fun editProject(editor: Project.() -> Project): AppState {
        val edited = project!!.editor()
        return copy(project = edited, history = history.push(edited))
    }

    fun editNonNullProject(editor: Project.() -> Project?): AppState {
        val edited = project!!.editor() ?: return this
        return copy(project = edited, history = history.push(edited))
    }

    fun editEntry(editedEntry: EditedEntry) = editProject { updateEntry(editedEntry) }

    fun undo(): AppState {
        val history = history.undo()
        return copy(project = history.current, history = history)
    }

    fun redo(): AppState {
        val history = history.redo()
        return copy(project = history.current, history = history)
    }

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
    val hasUnsavedChanges get() = projectWriteStatus == ProjectWriteStatus.Changed

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

    fun jumpToEntry(sampleName: String, entryIndex: Int) = editProject {
        project!!.copy(
            currentSampleName = sampleName,
            currentEntryIndex = entryIndex
        )
    }

    fun openEditEntryNameDialog(
        duplicate: Boolean,
        showSnackbar: (String) -> Unit
    ): AppState {
        val sampleName = project!!.currentSampleName
        val index = project.currentEntryIndex
        val entry = project.currentEntry
        val invalidOptions = if (project.labelerConf.allowSameNameEntry) {
            listOf()
        } else {
            project.allEntries.map { it.name }
                .run { if (!duplicate) minus(entry.name) else this }
        }
        return openEmbeddedDialog(
            EditEntryNameDialogArgs(
                sampleName = sampleName,
                index = index,
                initial = entry.name,
                invalidOptions = invalidOptions,
                showSnackbar = showSnackbar,
                duplicate = duplicate
            )
        )
    }

    fun renameEntry(sampleName: String, index: Int, newName: String) = editProject {
        renameEntry(sampleName, index, newName)
    }

    fun duplicateEntry(sampleName: String, index: Int, newName: String) = editProject {
        duplicateEntry(sampleName, index, newName).copy(currentEntryIndex = index + 1)
    }

    val canRemoveCurrentEntry
        get() = project?.let {
            it.entriesBySampleName.getValue(it.currentSampleName).size > 1
        } == true

    fun confirmIfRemoveCurrentEntry() = openEmbeddedDialog(CommonConfirmationDialogAction.RemoveCurrentEntry)
    fun removeCurrentEntry() = editProject { removeCurrentEntry() }

    fun projectContentChanged() = copy(projectWriteStatus = ProjectWriteStatus.Changed)
    fun projectPathChanged() = copy(projectWriteStatus = ProjectWriteStatus.Updated)

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
