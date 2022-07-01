package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

interface AppDialogState {
    val isShowingOpenProjectDialog: Boolean
    val isShowingSaveAsProjectDialog: Boolean
    val isShowingExportDialog: Boolean
    val pendingActionAfterSaved: AppState.PendingActionAfterSaved?
    val embeddedDialog: EmbeddedDialogArgs?

    fun requestOpenProject()
    fun openOpenProjectDialog()
    fun closeOpenProjectDialog()
    fun requestOpenRecentProject(scope: CoroutineScope, file: File, appState: AppState)
    fun openSaveAsProjectDialog()
    fun closeSaveAsProjectDialog()
    fun requestExport()
    fun openExportDialog()
    fun closeExportDialog()
    fun putPendingActionAfterSaved(action: AppState.PendingActionAfterSaved?)
    fun clearPendingActionAfterSaved()
    fun openEmbeddedDialog(args: EmbeddedDialogArgs)
    fun openJumpToEntryDialog()
    fun openEditEntryNameDialog(duplicate: Boolean, scope: CoroutineScope)
    fun askIfSaveBeforeExit()
    fun confirmIfRemoveCurrentEntry()
    fun confirmIfLoadAutoSavedProject(file: File)
    fun closeEmbeddedDialog()
    fun closeAllDialogs()

    fun anyDialogOpening() =
        isShowingExportDialog || isShowingSaveAsProjectDialog || isShowingExportDialog || embeddedDialog != null
}

class AppDialogStateImpl(
    private val appUnsavedChangesState: AppUnsavedChangesState,
    private val projectStore: ProjectStore,
    private val snackbarHostState: SnackbarHostState
) : AppDialogState {
    override var isShowingOpenProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingSaveAsProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingExportDialog: Boolean by mutableStateOf(false)
    override var pendingActionAfterSaved: AppState.PendingActionAfterSaved? by mutableStateOf(null)
    override var embeddedDialog: EmbeddedDialogArgs? by mutableStateOf(null)

    private val hasUnsavedChanges get() = appUnsavedChangesState.hasUnsavedChanges

    override fun requestOpenProject() = if (hasUnsavedChanges) askIfSaveBeforeOpenProject() else openOpenProjectDialog()

    private fun askIfSaveBeforeOpenProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsOpening)

    override fun openOpenProjectDialog() {
        isShowingOpenProjectDialog = true
    }

    override fun closeOpenProjectDialog() {
        isShowingOpenProjectDialog = false
    }

    override fun requestOpenRecentProject(scope: CoroutineScope, file: File, appState: AppState) =
        if (hasUnsavedChanges) {
            askIfSaveBeforeOpenRecentProject(scope, file)
        } else {
            loadProject(scope, file, appState)
        }

    private fun askIfSaveBeforeOpenRecentProject(scope: CoroutineScope, file: File) =
        openEmbeddedDialog(AskIfSaveDialogPurpose.IsOpeningRecent(scope, file))

    override fun openSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = true
    }

    override fun closeSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = false
    }

    override fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
    private fun askIfSaveBeforeExport() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsExporting)

    override fun openExportDialog() {
        isShowingExportDialog = true
    }

    override fun closeExportDialog() {
        isShowingExportDialog = false
    }

    override fun putPendingActionAfterSaved(action: AppState.PendingActionAfterSaved?) {
        pendingActionAfterSaved = action
    }

    override fun clearPendingActionAfterSaved() {
        pendingActionAfterSaved = null
    }

    override fun openEmbeddedDialog(args: EmbeddedDialogArgs) {
        embeddedDialog = args
    }

    override fun openJumpToEntryDialog() = openEmbeddedDialog(JumpToEntryDialogArgs(projectStore.requireProject()))

    override fun openEditEntryNameDialog(
        duplicate: Boolean,
        scope: CoroutineScope
    ) {
        val project = projectStore.requireProject()
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
                showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } },
                duplicate = duplicate
            )
        )
    }

    override fun askIfSaveBeforeExit() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)
    override fun confirmIfRemoveCurrentEntry() = openEmbeddedDialog(CommonConfirmationDialogAction.RemoveCurrentEntry)
    override fun confirmIfLoadAutoSavedProject(file: File) =
        openEmbeddedDialog(CommonConfirmationDialogAction.LoadAutoSavedProject(file))

    override fun closeEmbeddedDialog() {
        embeddedDialog = null
    }

    override fun closeAllDialogs() {
        isShowingOpenProjectDialog = false
        isShowingSaveAsProjectDialog = false
        isShowingExportDialog = false
        embeddedDialog = null
    }
}
