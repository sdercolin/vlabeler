package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import kotlinx.coroutines.CoroutineScope
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
    fun closeEmbeddedDialog()
    fun closeAllDialogs()
}

class AppDialogStateImpl(private val appUnsavedChangesState: AppUnsavedChangesState) : AppDialogState {
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

    override  fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
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