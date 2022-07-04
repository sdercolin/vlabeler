package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.ErrorDialogResult
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgsResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AppState(
    val playerState: PlayerState,
    val player: Player,
    val keyboardViewModel: KeyboardViewModel,
    val scrollFitViewModel: ScrollFitViewModel,
    val appRecordStore: AppRecordStore,
    val snackbarHostState: SnackbarHostState,
    appConf: MutableState<AppConf>,
    val availableLabelerConfs: List<LabelerConf>,
    private val plugins: List<Plugin>,
    viewState: AppViewState = AppViewStateImpl(appRecordStore),
    screenState: AppScreenState = AppScreenStateImpl(),
    projectStore: ProjectStore = ProjectStoreImpl(screenState, scrollFitViewModel),
    unsavedChangesState: AppUnsavedChangesState = AppUnsavedChangesStateImpl(),
    dialogState: AppDialogState = AppDialogStateImpl(unsavedChangesState, projectStore, snackbarHostState)
) : AppViewState by viewState,
    AppScreenState by screenState,
    ProjectStore by projectStore,
    AppUnsavedChangesState by unsavedChangesState,
    AppDialogState by dialogState {

    var appConf: AppConf by appConf
        private set

    val appRecordFlow: StateFlow<AppRecord> get() = appRecordStore.stateFlow

    var isBusy: Boolean by mutableStateOf(false)
        private set
    var shouldExit: Boolean by mutableStateOf(false)
        private set

    private fun reset() {
        clearProject()
        changeScreen(Screen.Starter)
        clearPendingActionAfterSaved()
        projectClosed()
    }

    fun getPlugins(type: Plugin.Type) = plugins.filter { it.type == type }

    private fun changeScreen(screen: Screen) {
        this.screen = screen
        closeAllDialogs()
    }

    fun addRecentProject(file: File) {
        appRecordStore.update { addRecent(file.absolutePath) }
    }

    fun clearRecentProjects() {
        appRecordStore.update { copy(recentProjects = listOf()) }
    }

    fun openEditor(project: Project) {
        newProject(project)
        val editor = EditorState(
            project = project,
            appState = this
        )
        changeScreen(Screen.Editor(editor))
    }

    fun requestOpenProjectCreator() = if (hasUnsavedChanges) askIfSaveBeforeCreateProject() else openProjectCreator()
    private fun askIfSaveBeforeCreateProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsCreatingNew)
    private fun openProjectCreator() = changeScreen(Screen.ProjectCreator)
    fun closeProjectCreator() = reset()

    fun requestCloseProject() = if (hasUnsavedChanges) askIfSaveBeforeCloseProject() else reset()
    private fun askIfSaveBeforeCloseProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsClosing)

    fun requestSave(pendingAction: PendingActionAfterSaved? = null) {
        requestProjectSave()
        putPendingActionAfterSaved(pendingAction)
    }

    private fun takeAskIfSaveResult(result: AskIfSaveDialogResult) =
        if (result.save) {
            requestSave(result.actionAfterSaved)
        } else consumePendingActionAfterSaved(result.actionAfterSaved)

    fun notifySaved() {
        projectSaved()
        consumePendingActionAfterSaved(pendingActionAfterSaved)
    }

    private fun consumePendingActionAfterSaved(action: PendingActionAfterSaved?) = when (action) {
        PendingActionAfterSaved.Open -> openOpenProjectDialog()
        PendingActionAfterSaved.Export -> openExportDialog()
        PendingActionAfterSaved.Close -> reset()
        PendingActionAfterSaved.CreatingNew -> openProjectCreator()
        PendingActionAfterSaved.Exit -> exit()
        is PendingActionAfterSaved.OpenRecent -> loadProject(action.scope, action.file, this)
        null -> Unit
    }

    fun handleDialogResult(
        result: EmbeddedDialogResult,
        mainScope: CoroutineScope
    ) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (result) {
            is SetResolutionDialogResult -> changeResolution(result.newValue)
            is AskIfSaveDialogResult -> takeAskIfSaveResult(result)
            is JumpToEntryDialogArgsResult -> {
                jumpToEntry(result.index)
            }
            is EditEntryNameDialogResult -> run {
                if (result.duplicate) {
                    duplicateEntry(result.index, result.name)
                } else {
                    renameEntry(result.index, result.name)
                }
            }
            is CommonConfirmationDialogResult -> when (val action = result.action) {
                is CommonConfirmationDialogAction.RemoveCurrentEntry -> removeCurrentEntry()
                is CommonConfirmationDialogAction.LoadAutoSavedProject -> {
                    loadProject(mainScope, action.file, this)
                    hasLoadedAutoSavedProject = true
                }
            }
            is ErrorDialogResult -> Unit
            else -> TODO("Dialog result handler is not implemented")
        }
    }

    fun showProgress() {
        isBusy = true
    }

    fun hideProgress() {
        isBusy = false
    }

    private fun changeResolution(newValue: Int) {
        editor?.changeResolution(newValue)
    }

    fun checkAutoSavedProject() {
        val file = getAutoSavedProjectFile()
        if (file != null) {
            confirmIfLoadAutoSavedProject(file)
        }
    }

    fun requestExit() = if (hasUnsavedChanges) askIfSaveBeforeExit() else exit()

    private fun exit() {
        discardAutoSavedProjects()
        shouldExit = true
    }

    val isEditorActive
        get() = project != null && screen is Screen.Editor && !anyDialogOpening()

    sealed class PendingActionAfterSaved {
        object Open : PendingActionAfterSaved()
        class OpenRecent(val scope: CoroutineScope, val file: File) : PendingActionAfterSaved()
        object Export : PendingActionAfterSaved()
        object Close : PendingActionAfterSaved()
        object CreatingNew : PendingActionAfterSaved()
        object Exit : PendingActionAfterSaved()
    }
}
