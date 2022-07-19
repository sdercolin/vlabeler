@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogRequest
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import com.sdercolin.vlabeler.util.getCacheDir
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

interface AppDialogState {

    fun initDialogState(appState: AppState)

    val isShowingOpenProjectDialog: Boolean
    val isShowingSaveAsProjectDialog: Boolean
    val isShowingExportDialog: Boolean
    val isShowingSampleListDialog: Boolean
    val isShowingSampleDirectoryRedirectDialog: Boolean
    val pendingActionAfterSaved: AppState.PendingActionAfterSaved?
    val embeddedDialog: EmbeddedDialogRequest<*>?

    fun requestOpenProject()
    fun openOpenProjectDialog()
    fun closeOpenProjectDialog()
    fun requestOpenRecentProject(scope: CoroutineScope, file: File)
    fun openSaveAsProjectDialog()
    fun closeSaveAsProjectDialog()
    fun requestExport()
    fun openExportDialog()
    fun closeExportDialog()
    fun putPendingActionAfterSaved(action: AppState.PendingActionAfterSaved?)
    fun clearPendingActionAfterSaved()
    fun <T : EmbeddedDialogArgs> openEmbeddedDialog(args: T)
    suspend fun <T : EmbeddedDialogArgs> awaitEmbeddedDialog(args: T): EmbeddedDialogResult<T>?
    fun openJumpToEntryDialog()
    fun openEditEntryNameDialog(index: Int, purpose: InputEntryNameDialogPurpose)
    fun askIfSaveBeforeExit()
    fun confirmIfRemoveCurrentEntry(isLastEntry: Boolean)
    fun confirmIfLoadAutoSavedProject(file: File)
    fun confirmIfRedirectSampleDirectory(currentDirectory: File)
    fun openSampleListDialog()
    fun closeSampleListDialog()
    fun openSampleDirectoryRedirectDialog()
    fun closeSampleDirectoryRedirectDialog()
    fun requestClearCaches(scope: CoroutineScope)
    fun clearCachesAndReopen(scope: CoroutineScope)

    fun closeEmbeddedDialog()
    fun closeAllDialogs()

    fun anyDialogOpening() =
        isShowingExportDialog || isShowingSaveAsProjectDialog || isShowingExportDialog ||
            isShowingSampleListDialog || isShowingSampleDirectoryRedirectDialog || embeddedDialog != null
}

class AppDialogStateImpl(
    private val appUnsavedChangesState: AppUnsavedChangesState,
    private val projectStore: ProjectStore,
    private val snackbarHostState: SnackbarHostState
) : AppDialogState {
    private lateinit var state: AppState

    override fun initDialogState(appState: AppState) {
        state = appState
    }

    override var isShowingOpenProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingSaveAsProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingExportDialog: Boolean by mutableStateOf(false)
    override var isShowingSampleListDialog: Boolean by mutableStateOf(false)
    override var isShowingSampleDirectoryRedirectDialog: Boolean by mutableStateOf(false)
    override var pendingActionAfterSaved: AppState.PendingActionAfterSaved? by mutableStateOf(null)
    override var embeddedDialog: EmbeddedDialogRequest<*>? by mutableStateOf(null)

    private val hasUnsavedChanges get() = appUnsavedChangesState.hasUnsavedChanges

    override fun requestOpenProject() = if (hasUnsavedChanges) askIfSaveBeforeOpenProject() else openOpenProjectDialog()

    private fun askIfSaveBeforeOpenProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsOpening)

    override fun openOpenProjectDialog() {
        isShowingOpenProjectDialog = true
    }

    override fun closeOpenProjectDialog() {
        isShowingOpenProjectDialog = false
    }

    override fun requestOpenRecentProject(scope: CoroutineScope, file: File) =
        if (hasUnsavedChanges) {
            askIfSaveBeforeOpenRecentProject(file)
        } else {
            loadProject(scope, file, state)
        }

    private fun askIfSaveBeforeOpenRecentProject(file: File) =
        openEmbeddedDialog(AskIfSaveDialogPurpose.IsOpeningRecent(file))

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

    override fun <T : EmbeddedDialogArgs> openEmbeddedDialog(args: T) {
        embeddedDialog = EmbeddedDialogRequest(args) {
            state.closeEmbeddedDialog()
            if (it != null) state.handleDialogResult(it)
        }
    }

    override suspend fun <T : EmbeddedDialogArgs> awaitEmbeddedDialog(args: T): EmbeddedDialogResult<T>? =
        suspendCancellableCoroutine { continuation ->
            val request = EmbeddedDialogRequest(args) {
                state.closeEmbeddedDialog()
                continuation.resume(it) { t ->
                    if (t !is CancellationException) Log.error(t)
                }
            }
            embeddedDialog = request
        }

    override fun openJumpToEntryDialog() = openEmbeddedDialog(JumpToEntryDialogArgs(projectStore.requireProject()))

    override fun openEditEntryNameDialog(index: Int, purpose: InputEntryNameDialogPurpose) {
        val project = projectStore.requireProject()
        val entry = project.entries[index]
        val invalidOptions = if (project.labelerConf.allowSameNameEntry) {
            listOf()
        } else {
            project.entries.map { it.name }
                .run { if (purpose == InputEntryNameDialogPurpose.Rename) minus(entry.name) else this }
        }
        openEmbeddedDialog(
            InputEntryNameDialogArgs(
                index = index,
                initial = entry.name,
                invalidOptions = invalidOptions,
                showSnackbar = { state.mainScope.launch { snackbarHostState.showSnackbar(it) } },
                purpose = purpose
            )
        )
    }

    override fun askIfSaveBeforeExit() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)

    override fun confirmIfRemoveCurrentEntry(isLastEntry: Boolean) =
        openEmbeddedDialog(CommonConfirmationDialogAction.RemoveCurrentEntry(isLastEntry))

    override fun confirmIfLoadAutoSavedProject(file: File) =
        openEmbeddedDialog(CommonConfirmationDialogAction.LoadAutoSavedProject(file))

    override fun confirmIfRedirectSampleDirectory(currentDirectory: File) {
        openEmbeddedDialog(CommonConfirmationDialogAction.RedirectSampleDirectory(currentDirectory))
    }

    override fun openSampleListDialog() {
        isShowingSampleListDialog = true
    }

    override fun closeSampleListDialog() {
        isShowingSampleListDialog = false
    }

    override fun openSampleDirectoryRedirectDialog() {
        isShowingSampleDirectoryRedirectDialog = true
    }

    override fun closeSampleDirectoryRedirectDialog() {
        isShowingSampleDirectoryRedirectDialog = false
    }

    override fun requestClearCaches(scope: CoroutineScope) =
        if (hasUnsavedChanges) askIfSaveBeforeClearCaches() else clearCachesAndReopen(scope)

    private fun askIfSaveBeforeClearCaches() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsClearingCaches)

    override fun clearCachesAndReopen(scope: CoroutineScope) {
        projectStore.requireProject().getCacheDir().deleteRecursively()
        loadProject(scope, projectStore.requireProject().projectFile, state)
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
