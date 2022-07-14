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
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.ErrorDialogResult
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogResult
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgsResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.util.getDefaultNewEntryName
import com.sdercolin.vlabeler.util.toFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class AppState(
    val mainScope: CoroutineScope,
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
    projectStore: ProjectStore = ProjectStoreImpl(appConf, screenState, scrollFitViewModel),
    unsavedChangesState: AppUnsavedChangesState = AppUnsavedChangesStateImpl(),
    dialogState: AppDialogState = AppDialogStateImpl(unsavedChangesState, projectStore, snackbarHostState)
) : AppViewState by viewState,
    AppScreenState by screenState,
    ProjectStore by projectStore,
    AppUnsavedChangesState by unsavedChangesState,
    AppDialogState by dialogState {

    init {
        initDialogState(this)
    }

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
        editor?.chartStore?.clear()
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
    private fun openProjectCreator() {
        reset()
        changeScreen(Screen.ProjectCreator)
    }

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

    fun requestCutEntry(index: Int, position: Float, player: Player, sampleInfo: SampleInfo) {
        mainScope.launch {
            val sourceEntry = requireProject().entries[index]
            val showSnackbar = { message: String ->
                mainScope.launch { snackbarHostState.showSnackbar(message) }
                Unit
            }

            val actions = appConf.editor.scissorsActions
            if (actions.play != null) {
                val startFrame = toFrame(sourceEntry.start, sampleInfo.sampleRate)
                val endFrame = toFrame(sourceEntry.end, sampleInfo.sampleRate)
                val cutFrame = toFrame(position, sampleInfo.sampleRate)
                when (actions.play) {
                    AppConf.ScissorsActions.Target.Former -> {
                        player.playSection(startFrame, cutFrame)
                    }
                    AppConf.ScissorsActions.Target.Latter -> {
                        player.playSection(cutFrame, endFrame)
                    }
                }
            }

            val rename = if (actions.askForName == AppConf.ScissorsActions.Target.Former) {
                val invalidOptions = if (requireProject().labelerConf.allowSameNameEntry) {
                    listOf()
                } else {
                    requireProject().entries.map { it.name }.minus(sourceEntry.name)
                }
                val result = awaitEmbeddedDialog(
                    InputEntryNameDialogArgs(
                        index = index,
                        initial = sourceEntry.name,
                        invalidOptions = invalidOptions,
                        showSnackbar = showSnackbar,
                        purpose = InputEntryNameDialogPurpose.CutFormer
                    )
                )
                (result as InputEntryNameDialogResult?)?.name ?: return@launch
            } else null
            val newName = if (actions.askForName == AppConf.ScissorsActions.Target.Latter) {
                val invalidOptions = if (requireProject().labelerConf.allowSameNameEntry) {
                    listOf()
                } else {
                    requireProject().entries.map { it.name }
                }

                val result = awaitEmbeddedDialog(
                    InputEntryNameDialogArgs(
                        index = index + 1,
                        initial = "",
                        invalidOptions = invalidOptions,
                        showSnackbar = showSnackbar,
                        purpose = InputEntryNameDialogPurpose.CutLatter
                    )
                )
                (result as InputEntryNameDialogResult?)?.name ?: return@launch
            } else getDefaultNewEntryName(
                sourceEntry.name,
                requireProject().entries.map { it.name },
                requireProject().labelerConf.allowSameNameEntry
            )
            val targetIndex = when (actions.goTo) {
                AppConf.ScissorsActions.Target.Former -> index
                AppConf.ScissorsActions.Target.Latter -> index + 1
                null -> null
            }
            cutEntry(index, position, rename, newName, targetIndex)
        }
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

    fun handleDialogResult(result: EmbeddedDialogResult<*>) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (result) {
            is SetResolutionDialogResult -> changeResolution(result.newValue)
            is AskIfSaveDialogResult -> takeAskIfSaveResult(result)
            is JumpToEntryDialogArgsResult -> {
                jumpToEntry(result.index)
            }
            is InputEntryNameDialogResult -> run {
                when (result.purpose) {
                    InputEntryNameDialogPurpose.Rename -> renameEntry(result.index, result.name)
                    InputEntryNameDialogPurpose.Duplicate -> duplicateEntry(result.index, result.name)
                    InputEntryNameDialogPurpose.CutFormer -> {
                        // handled on caller side
                    }
                    InputEntryNameDialogPurpose.CutLatter -> {
                        // handled on caller side
                    }
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

    val isEditorActive get() = project != null && screen is Screen.Editor && !anyDialogOpening()

    val isScrollFitEnabled get() = isMarkerDisplayed && editor?.sampleInfoResult?.getOrNull() != null

    sealed class PendingActionAfterSaved {
        object Open : PendingActionAfterSaved()
        class OpenRecent(val scope: CoroutineScope, val file: File) : PendingActionAfterSaved()
        object Export : PendingActionAfterSaved()
        object Close : PendingActionAfterSaved()
        object CreatingNew : PendingActionAfterSaved()
        object Exit : PendingActionAfterSaved()
    }
}
