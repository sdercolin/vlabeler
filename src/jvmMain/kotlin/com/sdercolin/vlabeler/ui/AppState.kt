package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import com.sdercolin.vlabeler.ui.editor.EditedEntry

class AppState {

    var project: Project? by mutableStateOf(null)
    var history: ProjectHistory by mutableStateOf(ProjectHistory())
    var screen: Screen by mutableStateOf(Screen.Starter)
    var isShowingOpenProjectDialog: Boolean by mutableStateOf(false)
    var isShowingSaveAsProjectDialog: Boolean by mutableStateOf(false)
    var isShowingExportDialog: Boolean by mutableStateOf(false)
    var pendingActionAfterSaved: PendingActionAfterSaved? by mutableStateOf(null)
    var embeddedDialog: EmbeddedDialogArgs? by mutableStateOf(null)

    /**
     * Describes the update status between [Project] state and project file
     */
    var projectWriteStatus: ProjectWriteStatus by mutableStateOf(ProjectWriteStatus.Updated)
    var isBusy: Boolean by mutableStateOf(false)
    var shouldExit: Boolean by mutableStateOf(false)

    private fun reset() {
        project = null
        history = ProjectHistory()
        screen = Screen.Starter
        isShowingExportDialog = false
        isShowingSaveAsProjectDialog = false
        isShowingExportDialog = false
        pendingActionAfterSaved = null
        embeddedDialog = null
    }

    val hasProject get() = project != null
    fun openProject(newProject: Project) {
        project = newProject
        history = ProjectHistory.new(newProject)
        screen = Screen.Editor
    }

    fun editProject(editor: Project.() -> Project) {
        val edited = project!!.editor()
        project = edited
        history = history.push(edited)
    }

    fun editNonNullProject(editor: Project.() -> Project?) {
        val edited = project!!.editor() ?: return
        project = edited
        history = history.push(edited)
    }

    fun editEntry(editedEntry: EditedEntry) = editProject { updateEntry(editedEntry) }

    fun undo() {
        history = history.undo()
        project = history.current
    }

    fun redo() {
        history = history.redo()
        project = history.current
    }

    fun requestOpenProjectCreator() = if (hasUnsavedChanges) askIfSaveBeforeCreateProject() else openProjectCreator()
    private fun askIfSaveBeforeCreateProject() {
        embeddedDialog = AskIfSaveDialogPurpose.IsCreatingNew
    }

    private fun openProjectCreator() {
        screen = Screen.ProjectCreator
    }

    fun closeProjectCreator() = reset()

    fun requestOpenProject() = if (hasUnsavedChanges) askIfSaveBeforeOpenProject() else openOpenProjectDialog()
    private fun askIfSaveBeforeOpenProject() {
        embeddedDialog = AskIfSaveDialogPurpose.IsOpening
    }

    fun openOpenProjectDialog() {
        isShowingOpenProjectDialog = true
    }

    fun closeOpenProjectDialog() {
        isShowingOpenProjectDialog = false
    }

    fun openSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = true
    }

    fun closeSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = false
    }

    fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
    private fun askIfSaveBeforeExport() {
        embeddedDialog = AskIfSaveDialogPurpose.IsExporting
    }

    private fun openExportDialog() {
        isShowingExportDialog = true
    }

    fun closeExportDialog() {
        isShowingExportDialog = false
    }

    fun requestCloseProject() = if (hasUnsavedChanges) askIfSaveBeforeCloseProject() else reset()
    private fun askIfSaveBeforeCloseProject() {
        embeddedDialog = AskIfSaveDialogPurpose.IsClosing
    }

    val hasUnsavedChanges get() = projectWriteStatus == ProjectWriteStatus.Changed

    fun requestSave(pendingAction: PendingActionAfterSaved? = null) {
        projectWriteStatus = ProjectWriteStatus.UpdateRequested
        pendingActionAfterSaved = pendingAction
    }

    fun takeAskIfSaveResult(result: AskIfSaveDialogResult) =
        if (result.save) {
            requestSave(result.actionAfterSaved)
        } else consumePendingActionAfterSaved(result.actionAfterSaved)

    fun notifySaved() {
        projectWriteStatus = ProjectWriteStatus.Updated
        consumePendingActionAfterSaved(pendingActionAfterSaved)
    }

    private fun consumePendingActionAfterSaved(action: PendingActionAfterSaved?) = when (action) {
        PendingActionAfterSaved.Open -> openOpenProjectDialog()
        PendingActionAfterSaved.Export -> openExportDialog()
        PendingActionAfterSaved.Close -> reset()
        PendingActionAfterSaved.CreatingNew -> openProjectCreator()
        PendingActionAfterSaved.Exit -> exit()
        null -> Unit
    }

    fun openEmbeddedDialog(args: EmbeddedDialogArgs) {
        embeddedDialog = args
    }

    fun closeEmbeddedDialog() {
        embeddedDialog = null
    }

    fun openJumpToEntryDialog() {
        embeddedDialog = JumpToEntryDialogArgs(project!!)
    }

    val canGoNextEntryOrSample get() = project?.run { currentEntryIndexInTotal < totalEntryCount - 1 } == true
    val canGoPreviousEntryOrSample get() = project?.run { currentEntryIndexInTotal > 0 } == true

    /**
     * @return true if it has switched to another sample
     */
    fun nextEntry(): Boolean {
        val previousProject = project
        editNonNullProject { nextEntry() }
        return project!!.hasSwitchedSample(previousProject)
    }

    /**
     * @return true if it has switched to another sample
     */
    fun previousEntry(): Boolean {
        val previous = project
        editNonNullProject { previousEntry() }
        return project!!.hasSwitchedSample(previous)
    }

    fun nextSample() = editNonNullProject { nextSample() }
    fun previousSample() = editNonNullProject { previousSample() }

    fun jumpToEntry(sampleName: String, entryIndex: Int) = editProject {
        project!!.copy(
            currentSampleName = sampleName,
            currentEntryIndex = entryIndex
        )
    }

    fun openEditEntryNameDialog(
        duplicate: Boolean,
        showSnackbar: (String) -> Unit
    ) {
        val project = project!!
        val sampleName = project.currentSampleName
        val index = project.currentEntryIndex
        val entry = project.currentEntry
        val invalidOptions = if (project.labelerConf.allowSameNameEntry) {
            listOf()
        } else {
            project.allEntries.map { it.name }
                .run { if (!duplicate) minus(entry.name) else this }
        }
        openEmbeddedDialog(
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

    fun projectContentChanged() {
        projectWriteStatus = ProjectWriteStatus.Changed
    }

    fun projectPathChanged() {
        projectWriteStatus = ProjectWriteStatus.Updated
    }

    fun startProcess() {
        isBusy = true
    }

    fun finishProcess() {
        isBusy = false
    }

    fun requestExit() = if (hasUnsavedChanges) askIfSaveBeforeExit() else exit()
    private fun askIfSaveBeforeExit() {
        embeddedDialog = AskIfSaveDialogPurpose.IsExiting
    }

    private fun exit() {
        shouldExit = true
    }

    val isEditorActive
        get() = project != null &&
            screen == Screen.Editor &&
            !isShowingOpenProjectDialog &&
            !isShowingSaveAsProjectDialog &&
            !isShowingExportDialog &&
            embeddedDialog == null

    enum class Screen {
        Starter,
        ProjectCreator,
        Editor
    }

    enum class ProjectWriteStatus {
        Updated,
        Changed,
        UpdateRequested
    }

    enum class PendingActionAfterSaved {
        Open,
        Export,
        Close,
        CreatingNew,
        Exit
    }
}

@Composable
fun rememberAppState() = remember { AppState() }
