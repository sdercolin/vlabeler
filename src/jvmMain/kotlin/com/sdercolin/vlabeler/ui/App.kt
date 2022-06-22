package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.shouldGoNextEntry
import com.sdercolin.vlabeler.env.shouldGoNextSample
import com.sdercolin.vlabeler.env.shouldGoPreviousEntry
import com.sdercolin.vlabeler.env.shouldGoPreviousSample
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.dialog.EditEntryNameDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgsResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.ui.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun App(
    mainScope: CoroutineScope,
    appConf: AppConf,
    availableLabelerConfs: List<LabelerConf>,
    appState: MutableState<AppState>,
    playerState: PlayerState,
    snackbarHostState: SnackbarHostState,
    keyboardViewModel: KeyboardViewModel,
    scrollFitViewModel: ScrollFitViewModel,
    player: Player
) {
    val labelerState = remember(appConf.painter.canvasResolution.default) {
        mutableStateOf(LabelerState(appConf.painter.canvasResolution.default))
    }
    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect {
            if (appState.value.isEditorActive.not()) return@collect
            val project = appState.value.project ?: return@collect
            val updated = when {
                it.shouldGoNextSample -> project.nextSample()
                it.shouldGoPreviousSample -> project.previousSample()
                it.shouldGoNextEntry -> project.nextEntry()
                it.shouldGoPreviousEntry -> project.previousEntry()
                else -> null
            } ?: return@collect
            appState.update { editProject { updated } }
            if (it.shouldGoNextSample || it.shouldGoPreviousSample) {
                scrollFitViewModel.emitNext()
            }
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = appState.value.project
        if (project != null && appState.value.isConfiguringNewProject.not()) {
            Editor(
                project = project,
                editProject = { appState.update { editProject { it } } },
                editEntry = { appState.update { editEntry(it) } },
                showDialog = { appState.update { openEmbeddedDialog(it) } },
                appConf = appConf,
                labelerState = labelerState,
                appState = appState,
                playerState = playerState,
                keyboardViewModel = keyboardViewModel,
                scrollFitViewModel = scrollFitViewModel,
                player = player
            )
        } else {
            Starter(
                appState = appState,
                requestNewProject = {
                    mainScope.launch {
                        saveProjectFile(it)
                        appState.update { openProject(it) }
                    }
                },
                availableLabelerConfs = availableLabelerConfs,
                snackbarHostState = snackbarHostState
            )
        }
        appState.value.embeddedDialog?.let { args ->
            EmbeddedDialog(args) { result ->
                appState.update { closeEmbeddedDialog() }
                if (result != null) handleDialogResult(result, labelerState, appState, scrollFitViewModel)
            }
        }
    }
}

private fun handleDialogResult(
    result: EmbeddedDialogResult,
    labelerState: MutableState<LabelerState>,
    appState: MutableState<AppState>,
    scrollFitViewModel: ScrollFitViewModel
) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.update { changeResolution(result.newValue) }
        is AskIfSaveDialogResult -> appState.update { takeAskIfSaveResult(result) }
        is JumpToEntryDialogArgsResult -> {
            appState.update { jumpToEntry(result.sampleName, result.index) }
            scrollFitViewModel.emitNext()
        }
        is EditEntryNameDialogResult -> appState.update {
            if (result.duplicate) {
                duplicateEntry(result.sampleName, result.index, result.name)
            } else {
                renameEntry(result.sampleName, result.index, result.name)
            }
        }
        is CommonConfirmationDialogResult -> when (result.action) {
            CommonConfirmationDialogAction.RemoveCurrentEntry -> appState.update { removeCurrentEntry() }
        }
    }
}
