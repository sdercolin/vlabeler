@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.getSavedParamsFile
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogRequest
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialogState
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.runIf
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

interface AppDialogState {

    fun initDialogState(appState: AppState)

    val isShowingOpenProjectDialog: Boolean
    val isShowingSaveAsProjectDialog: Boolean
    val isShowingExportDialog: Boolean
    val isShowingPreferencesDialog: Boolean
    val isShowingSampleListDialog: Boolean
    val isShowingSampleDirectoryRedirectDialog: Boolean
    val isShowingPrerenderDialog: Boolean
    val isShowingAboutDialog: Boolean
    val isShowingLicenseDialog: Boolean
    val updaterDialogContent: Update?
    val macroPluginShownInDialog: Pair<Plugin, ParamMap>?
    val macroPluginReport: LocalizedJsonString?
    val customizableItemManagerTypeShownInDialog: CustomizableItem.Type?
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
    fun confirmIfRemoveCustomizableItem(state: CustomizableItemManagerDialogState<*>, item: CustomizableItem)
    fun openSampleListDialog()
    fun closeSampleListDialog()
    fun openSampleDirectoryRedirectDialog()
    fun closeSampleDirectoryRedirectDialog()
    fun openPrerenderDialog()
    fun closePrerenderDialog()
    fun openPreferencesDialog()
    fun closePreferencesDialog()
    fun openAboutDialog()
    fun closeAboutDialog()
    fun openLicenseDialog()
    fun closeLicenseDialog()
    fun openUpdaterDialog(update: Update)
    fun closeUpdaterDialog()
    fun openMacroPluginDialog(plugin: Plugin)
    fun updateMacroPluginDialogInputParams(params: ParamMap)
    fun closeMacroPluginDialog()
    fun showMacroPluginReport(report: LocalizedJsonString)
    fun closeMacroPluginReport()
    fun openCustomizableItemManagerDialog(type: CustomizableItem.Type)
    fun closeCustomizableItemManagerDialog()
    fun requestClearCaches(scope: CoroutineScope)
    fun clearCachesAndReopen(scope: CoroutineScope)

    fun closeEmbeddedDialog()
    fun closeAllDialogs()

    fun anyDialogOpening() =
        isShowingExportDialog || isShowingSaveAsProjectDialog || isShowingExportDialog || isShowingPreferencesDialog ||
            isShowingSampleListDialog || isShowingSampleDirectoryRedirectDialog || isShowingPrerenderDialog ||
            macroPluginShownInDialog != null || macroPluginReport != null ||
            customizableItemManagerTypeShownInDialog != null || embeddedDialog != null

    fun anyDialogOpeningExceptMacroPluginManager() =
        isShowingExportDialog || isShowingSaveAsProjectDialog || isShowingExportDialog || isShowingPreferencesDialog ||
            isShowingSampleListDialog || isShowingSampleDirectoryRedirectDialog || isShowingPrerenderDialog ||
            macroPluginShownInDialog != null || macroPluginReport != null ||
            (
                customizableItemManagerTypeShownInDialog != null &&
                    customizableItemManagerTypeShownInDialog != CustomizableItem.Type.MacroPlugin
                ) ||
            embeddedDialog != null
}

class AppDialogStateImpl(
    private val appUnsavedChangesState: AppUnsavedChangesState,
    private val projectStore: ProjectStore,
    private val snackbarState: AppSnackbarState,
) : AppDialogState {
    private lateinit var state: AppState
    private lateinit var scope: CoroutineScope

    override fun initDialogState(appState: AppState) {
        state = appState
        scope = appState.mainScope
    }

    override var isShowingOpenProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingSaveAsProjectDialog: Boolean by mutableStateOf(false)
    override var isShowingExportDialog: Boolean by mutableStateOf(false)
    override var isShowingPreferencesDialog: Boolean by mutableStateOf(false)
    override var isShowingSampleListDialog: Boolean by mutableStateOf(false)
    override var isShowingSampleDirectoryRedirectDialog: Boolean by mutableStateOf(false)
    override var isShowingPrerenderDialog: Boolean by mutableStateOf(false)
    override var isShowingAboutDialog: Boolean by mutableStateOf(false)
    override var isShowingLicenseDialog: Boolean by mutableStateOf(false)
    override var updaterDialogContent: Update? by mutableStateOf(null)
    override var macroPluginShownInDialog: Pair<Plugin, ParamMap>? by mutableStateOf(null)
    override var macroPluginReport: LocalizedJsonString? by mutableStateOf(null)
    override var customizableItemManagerTypeShownInDialog: CustomizableItem.Type? by mutableStateOf(null)
    override var pendingActionAfterSaved: AppState.PendingActionAfterSaved? by mutableStateOf(null)
    override var embeddedDialog: EmbeddedDialogRequest<*>? by mutableStateOf(null)

    private val hasUnsavedChanges get() = appUnsavedChangesState.hasUnsavedChanges

    override fun requestOpenProject() = if (hasUnsavedChanges) askIfSaveBeforeOpenProject() else openOpenProjectDialog()

    private fun askIfSaveBeforeOpenProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsOpening)

    override fun openOpenProjectDialog() {
        closeAllDialogs()
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
        closeAllDialogs()
        isShowingSaveAsProjectDialog = true
    }

    override fun closeSaveAsProjectDialog() {
        isShowingSaveAsProjectDialog = false
    }

    override fun requestExport() = if (hasUnsavedChanges) askIfSaveBeforeExport() else openExportDialog()
    private fun askIfSaveBeforeExport() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsExporting)

    override fun openExportDialog() {
        closeAllDialogs()
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

    private var awaitEmbeddedDialogContinuation: CancellableContinuation<*>? = null

    override suspend fun <T : EmbeddedDialogArgs> awaitEmbeddedDialog(args: T): EmbeddedDialogResult<T>? =
        suspendCancellableCoroutine { continuation ->
            awaitEmbeddedDialogContinuation = continuation
            val request = EmbeddedDialogRequest(args) {
                continuation.resume(it) { t ->
                    if (t !is CancellationException) Log.error(t)
                }
                state.closeEmbeddedDialog()
            }
            embeddedDialog = request
        }

    override fun openJumpToEntryDialog() = openEmbeddedDialog(
        JumpToEntryDialogArgs(projectStore.requireProject(), state.appConf.editor),
    )

    override fun openEditEntryNameDialog(index: Int, purpose: InputEntryNameDialogPurpose) {
        val project = projectStore.requireProject()
        val entry = project.currentModule.entries[index]
        val invalidOptions = if (project.labelerConf.allowSameNameEntry) {
            listOf()
        } else {
            project.currentModule.entries.map { it.name }
                .runIf(purpose == InputEntryNameDialogPurpose.Rename) { minus(entry.name) }
        }
        openEmbeddedDialog(
            InputEntryNameDialogArgs(
                index = index,
                initial = entry.name,
                invalidOptions = invalidOptions,
                showSnackbar = { state.mainScope.launch { snackbarState.showSnackbar(it) } },
                purpose = purpose,
            ),
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

    override fun confirmIfRemoveCustomizableItem(state: CustomizableItemManagerDialogState<*>, item: CustomizableItem) {
        openEmbeddedDialog(CommonConfirmationDialogAction.RemoveCustomizableItem(state, item))
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

    override fun openPrerenderDialog() {
        isShowingPrerenderDialog = true
    }

    override fun closePrerenderDialog() {
        isShowingPrerenderDialog = false
    }

    override fun openPreferencesDialog() {
        closeAllDialogs()
        isShowingPreferencesDialog = true
    }

    override fun closePreferencesDialog() {
        isShowingPreferencesDialog = false
    }

    override fun requestClearCaches(scope: CoroutineScope) =
        if (hasUnsavedChanges) askIfSaveBeforeClearCaches() else clearCachesAndReopen(scope)

    private fun askIfSaveBeforeClearCaches() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsClearingCaches)

    override fun openUpdaterDialog(update: Update) {
        updaterDialogContent = update
    }

    override fun closeUpdaterDialog() {
        updaterDialogContent = null
    }

    override fun openAboutDialog() {
        isShowingAboutDialog = true
    }

    override fun closeAboutDialog() {
        isShowingAboutDialog = false
    }

    override fun openLicenseDialog() {
        isShowingLicenseDialog = true
    }

    override fun closeLicenseDialog() {
        isShowingLicenseDialog = false
    }

    override fun openMacroPluginDialog(plugin: Plugin) {
        scope.launch(Dispatchers.IO) {
            macroPluginShownInDialog = plugin to plugin.loadSavedParams(plugin.getSavedParamsFile())
        }
    }

    override fun updateMacroPluginDialogInputParams(params: ParamMap) {
        macroPluginShownInDialog = macroPluginShownInDialog?.let { it.first to params }
    }

    override fun openCustomizableItemManagerDialog(type: CustomizableItem.Type) {
        closeAllDialogs()
        customizableItemManagerTypeShownInDialog = type
    }

    override fun closeCustomizableItemManagerDialog() {
        customizableItemManagerTypeShownInDialog = null
    }

    override fun closeMacroPluginDialog() {
        macroPluginShownInDialog = null
    }

    override fun showMacroPluginReport(report: LocalizedJsonString) {
        macroPluginReport = report
    }

    override fun closeMacroPluginReport() {
        macroPluginReport = null
    }

    override fun clearCachesAndReopen(scope: CoroutineScope) {
        SampleInfoRepository.clearMemory()
        projectStore.requireProject().getCacheDir().deleteRecursively()
        loadProject(scope, projectStore.requireProject().projectFile, state)
    }

    override fun closeEmbeddedDialog() {
        awaitEmbeddedDialogContinuation?.cancel()
        awaitEmbeddedDialogContinuation = null
        embeddedDialog = null
    }

    override fun closeAllDialogs() {
        isShowingOpenProjectDialog = false
        isShowingSaveAsProjectDialog = false
        isShowingExportDialog = false
        isShowingSampleListDialog = false
        isShowingPreferencesDialog = false
        isShowingSampleDirectoryRedirectDialog = false
        macroPluginShownInDialog = null
        macroPluginReport = null
        customizableItemManagerTypeShownInDialog = null
        closeEmbeddedDialog()
    }
}
