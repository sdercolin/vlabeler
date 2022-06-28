package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.io.openCreatedProject
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
import com.sdercolin.vlabeler.ui.editor.labeler.rememberLabelerState
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(
    mainScope: CoroutineScope,
    appState: AppState
) {
    val labelerState = rememberLabelerState(appState.appConf)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = appState.project
        when (appState.screen) {
            AppState.Screen.Starter -> Starter(mainScope, appState)
            AppState.Screen.ProjectCreator ->
                ProjectCreator(
                    create = { openCreatedProject(mainScope, it, appState) },
                    cancel = { appState.closeProjectCreator() },
                    availableLabelerConfs = appState.availableLabelerConfs,
                    snackbarHostState = appState.snackbarHostState
                )
            AppState.Screen.Editor -> if (project != null) {
                Editor(
                    project = project,
                    labelerState = labelerState,
                    appState = appState
                )
            }
        }
        appState.embeddedDialog?.let { args ->
            EmbeddedDialog(args) { result ->
                appState.closeEmbeddedDialog()
                if (result != null) handleDialogResult(result, labelerState, appState)
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
    appState: AppState
) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.changeResolution(result.newValue)
        is AskIfSaveDialogResult -> appState.takeAskIfSaveResult(result)
        is JumpToEntryDialogArgsResult -> {
            appState.jumpToEntry(result.sampleName, result.index)
            appState.scrollFitViewModel.emitNext()
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
