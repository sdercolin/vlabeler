package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.io.openCreatedProject
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgsResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.editor.Editor
import com.sdercolin.vlabeler.ui.editor.labeler.LabelerState
import com.sdercolin.vlabeler.ui.editor.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.labeler.rememberLabelerState
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(
    mainScope: CoroutineScope,
    appConf: AppConf,
    availableLabelerConfs: List<LabelerConf>,
    appState: AppState,
    playerState: PlayerState,
    appRecord: MutableState<AppRecord>,
    snackbarHostState: SnackbarHostState,
    keyboardViewModel: KeyboardViewModel,
    scrollFitViewModel: ScrollFitViewModel,
    player: Player
) {
    val labelerState = rememberLabelerState(appConf)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = appState.project
        when (appState.screen) {
            AppState.Screen.Starter -> Starter(
                mainScope = mainScope,
                appState = appState,
                appRecord = appRecord,
                availableLabelerConfs = availableLabelerConfs,
                snackbarHostState = snackbarHostState,
                scrollFitViewModel = scrollFitViewModel
            )
            AppState.Screen.ProjectCreator ->
                ProjectCreator(
                    create = { openCreatedProject(mainScope, it, appState, appRecord, scrollFitViewModel) },
                    cancel = { appState.closeProjectCreator() },
                    availableLabelerConfs = availableLabelerConfs,
                    snackbarHostState = snackbarHostState
                )
            AppState.Screen.Editor -> if (project != null) {
                Editor(
                    project = project,
                    editProject = { appState.editProject { it } },
                    editEntry = { appState.editEntry(it) },
                    showDialog = { appState.openEmbeddedDialog(it) },
                    appConf = appConf,
                    labelerState = labelerState,
                    appState = appState,
                    playerState = playerState,
                    snackbarHostState = snackbarHostState,
                    keyboardViewModel = keyboardViewModel,
                    scrollFitViewModel = scrollFitViewModel,
                    player = player
                )
            }
        }
        appState.embeddedDialog?.let { args ->
            EmbeddedDialog(args) { result ->
                appState.closeEmbeddedDialog()
                if (result != null) handleDialogResult(result, labelerState, appState, scrollFitViewModel)
            }
        }
    }
    if (appState.isBusy) {
        CircularProgress()
    }
}

private fun handleDialogResult(
    result: EmbeddedDialogResult,
    labelerState: LabelerState,
    appState: AppState,
    scrollFitViewModel: ScrollFitViewModel
) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.changeResolution(result.newValue)
        is AskIfSaveDialogResult -> appState.takeAskIfSaveResult(result)
        is JumpToEntryDialogArgsResult -> {
            appState.jumpToEntry(result.sampleName, result.index)
            scrollFitViewModel.emitNext()
        }
        is EditEntryNameDialogResult -> appState.run {
            if (result.duplicate) {
                duplicateEntry(result.sampleName, result.index, result.name)
            } else {
                renameEntry(result.sampleName, result.index, result.name)
            }
        }
        is CommonConfirmationDialogResult -> when (result.action) {
            CommonConfirmationDialogAction.RemoveCurrentEntry -> appState.removeCurrentEntry()
        }
    }
}
