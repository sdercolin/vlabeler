package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs

interface AppDialogState {
    val isShowingOpenProjectDialog: Boolean
    val isShowingSaveAsProjectDialog: Boolean
    val isShowingExportDialog: Boolean
    val pendingActionAfterSaved: AppState.PendingActionAfterSaved?
    val embeddedDialog: EmbeddedDialogArgs?

    fun openOpenProjectDialog()
    fun closeOpenProjectDialog()
    fun openSaveAsProjectDialog()
    fun closeSaveAsProjectDialog()
    fun openExportDialog()
    fun closeExportDialog()
    fun putPendingActionAfterSaved(action: AppState.PendingActionAfterSaved?)
    fun clearPendingActionAfterSaved()
    fun openEmbeddedDialog(args: EmbeddedDialogArgs)
    fun closeEmbeddedDialog()
    fun closeAllDialogs()
}

class AppDialogStateImpl : AppDialogState {
    override var isShowingOpenProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingSaveAsProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingExportDialog: Boolean by mutableStateOf(false)
    override var pendingActionAfterSaved: AppState.PendingActionAfterSaved? by mutableStateOf(null)
    override var embeddedDialog: EmbeddedDialogArgs? by mutableStateOf(null)

    override fun openOpenProjectDialog() {
        isShowingOpenProjectDialog = true
    }

    override fun closeOpenProjectDialog() {
        isShowingOpenProjectDialog = false
    }

    override fun openSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = true
    }

    override fun closeSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = false
    }

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