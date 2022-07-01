package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.io.loadProject
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgsResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.editor.EditedEntry
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    viewState: AppViewState = AppViewStateImpl(appRecordStore),
    unsavedChangesState: AppUnsavedChangesState = AppUnsavedChangesStateImpl(),
    dialogState: AppDialogState = AppDialogStateImpl(unsavedChangesState)
) : AppViewState by viewState,
    AppUnsavedChangesState by unsavedChangesState,
    AppDialogState by dialogState {

    private val editorState get() = (screen as? Screen.Editor)?.editorState

    var appConf: AppConf by appConf
        private set

    val appRecord: AppRecord get() = appRecordStore.value

    private val projectState: MutableState<Project?> = mutableStateOf(null)
    var project: Project?
        get() = projectState.value
        private set(value) {
            if (value != null) {
                editorState?.updateProject(value)
            }
            projectState.value = value
        }
    var history: ProjectHistory by mutableStateOf(ProjectHistory())
        private set

    private val screenState = mutableStateOf<Screen>(Screen.Starter)
    var screen: Screen
        get() = screenState.value
        private set(value) {
            closeAllDialogs()
            screenState.value = value
        }

    var isBusy: Boolean by mutableStateOf(false)
        private set
    var shouldExit: Boolean by mutableStateOf(false)
        private set

    private fun reset() {
        project = null
        history = ProjectHistory()
        screen = Screen.Starter
        closeAllDialogs()
        clearPendingActionAfterSaved()
    }

    fun addRecentProject(file: File) {
        appRecordStore.update { addRecent(file.absolutePath) }
    }

    fun clearRecentProjects() {
        appRecordStore.update { copy(recentProjects = listOf()) }
    }

    val hasProject get() = project != null
    fun openProject(newProject: Project) {
        project = newProject
        history = ProjectHistory.new(newProject)
        val editor = EditorState(
            project = newProject,
            appState = this
        )
        screen = Screen.Editor(editor)
    }

    fun editProject(editor: Project.() -> Project) {
        val edited = project!!.editor()
        project = edited
        history = history.push(edited)
    }

    private fun editNonNullProject(editor: Project.() -> Project?) {
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
    private fun askIfSaveBeforeCreateProject() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsCreatingNew)

    private fun openProjectCreator() {
        screen = Screen.ProjectCreator
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
        result: EmbeddedDialogResult
    ) {
        when (result) {
            is SetResolutionDialogResult -> changeResolution(result.newValue)
            is AskIfSaveDialogResult -> takeAskIfSaveResult(result)
            is JumpToEntryDialogArgsResult -> {
                jumpToEntry(result.sampleName, result.index)
            }
            is EditEntryNameDialogResult -> run {
                if (result.duplicate) {
                    duplicateEntry(result.sampleName, result.index, result.name)
                } else {
                    renameEntry(result.sampleName, result.index, result.name)
                }
            }
            is CommonConfirmationDialogResult -> when (result.action) {
                CommonConfirmationDialogAction.RemoveCurrentEntry -> removeCurrentEntry()
            }
        }
    }

    fun openJumpToEntryDialog() = openEmbeddedDialog(JumpToEntryDialogArgs(project!!))

    val canGoNextEntryOrSample get() = project?.run { currentEntryIndexInTotal < totalEntryCount - 1 } == true
    val canGoPreviousEntryOrSample get() = project?.run { currentEntryIndexInTotal > 0 } == true

    fun nextEntry() {
        val previousProject = project
        editNonNullProject { nextEntry() }
        if (project!!.hasSwitchedSample(previousProject)) scrollFitViewModel.emitNext()
    }

    fun previousEntry() {
        val previous = project
        editNonNullProject { previousEntry() }
        if (project!!.hasSwitchedSample(previous)) scrollFitViewModel.emitNext()
    }

    fun nextSample() {
        editNonNullProject { nextSample() }
        scrollFitViewModel.emitNext()
    }

    fun previousSample() {
        editNonNullProject { previousSample() }
        scrollFitViewModel.emitNext()
    }

    fun jumpToEntry(sampleName: String, entryIndex: Int) {
        editProject {
            project!!.copy(
                currentSampleName = sampleName,
                currentEntryIndex = entryIndex
            )
        }
        scrollFitViewModel.emitNext()
    }

    fun openEditEntryNameDialog(
        duplicate: Boolean,
        scope: CoroutineScope
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
                showSnackbar = { scope.launch { snackbarHostState.showSnackbar(it) } },
                duplicate = duplicate
            )
        )
    }

    private fun renameEntry(sampleName: String, index: Int, newName: String) = editProject {
        renameEntry(sampleName, index, newName)
    }

    private fun duplicateEntry(sampleName: String, index: Int, newName: String) = editProject {
        duplicateEntry(sampleName, index, newName).copy(currentEntryIndex = index + 1)
    }

    val canRemoveCurrentEntry
        get() = project?.let {
            it.entriesBySampleName.getValue(it.currentSampleName).size > 1
        } == true

    fun confirmIfRemoveCurrentEntry() = openEmbeddedDialog(CommonConfirmationDialogAction.RemoveCurrentEntry)
    private fun removeCurrentEntry() = editProject { removeCurrentEntry() }

    fun showProgress() {
        isBusy = true
    }

    fun hideProgress() {
        isBusy = false
    }

    private fun changeResolution(newValue: Int) {
        editorState?.changeResolution(newValue)
    }

    fun requestExit() = if (hasUnsavedChanges) askIfSaveBeforeExit() else exit()
    private fun askIfSaveBeforeExit() = openEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)

    private fun exit() {
        shouldExit = true
    }

    val isEditorActive
        get() = project != null &&
            screen is Screen.Editor &&
            !isShowingOpenProjectDialog &&
            !isShowingSaveAsProjectDialog &&
            !isShowingExportDialog &&
            embeddedDialog == null

    sealed class Screen {
        object Starter : Screen()
        object ProjectCreator : Screen()
        class Editor(val editorState: EditorState) : Screen()
    }

    sealed class PendingActionAfterSaved {
        object Open : PendingActionAfterSaved()
        class OpenRecent(val scope: CoroutineScope, val file: File) : PendingActionAfterSaved()
        object Export : PendingActionAfterSaved()
        object Close : PendingActionAfterSaved()
        object CreatingNew : PendingActionAfterSaved()
        object Exit : PendingActionAfterSaved()
    }
}

@Composable
fun rememberAppState(
    playerState: PlayerState,
    player: Player,
    keyboardViewModel: KeyboardViewModel,
    scrollFitViewModel: ScrollFitViewModel,
    appRecordStore: AppRecordStore,
    snackbarHostState: SnackbarHostState,
    appConf: MutableState<AppConf>,
    availableLabelerConfs: List<LabelerConf>
) = remember {
    AppState(
        playerState,
        player,
        keyboardViewModel,
        scrollFitViewModel,
        appRecordStore,
        snackbarHostState,
        appConf,
        availableLabelerConfs
    )
}
