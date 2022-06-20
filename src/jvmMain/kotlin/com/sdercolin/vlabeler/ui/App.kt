package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
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
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun App(
    mainScope: CoroutineScope,
    appConf: AppConf,
    availableLabelerConfs: List<LabelerConf>,
    projectState: MutableState<Project?>,
    appState: MutableState<AppState>,
    playerState: PlayerState,
    keyboardViewModel: KeyboardViewModel,
    player: Player
) {
    val labelerState = remember(appConf.painter.canvasResolution.default) {
        mutableStateOf(LabelerState(appConf.painter.canvasResolution.default))
    }
    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect {
            if (appState.value.isEditorActive.not()) return@collect
            val project = projectState.value ?: return@collect
            val updated = when {
                it.shouldGoNextSample -> project.nextSample()
                it.shouldGoPreviousSample -> project.previousSample()
                it.shouldGoNextEntry -> project.nextEntry()
                it.shouldGoPreviousEntry -> project.previousEntry()
                else -> null
            } ?: return@collect
            projectState.update { updated }
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = projectState.value
        if (project != null && appState.value.isConfiguringNewProject.not()) {
            Editor(
                project = project,
                editProject = { projectState.updateNonNull { it }},
                editEntry = { projectState.update { project.updateEntry(it) } },
                showDialog = { appState.update { copy(embeddedDialog = it) } },
                appConf = appConf,
                labelerState = labelerState,
                appState = appState,
                playerState = playerState,
                keyboardViewModel = keyboardViewModel,
                player = player
            )
        } else {
            Starter(
                appState = appState,
                requestNewProject = {
                    mainScope.launch {
                        saveProjectFile(it)
                        appState.update { newFileOpened() }
                        projectState.update { it }
                    }
                },
                availableLabelerConfs = availableLabelerConfs
            )
        }
        appState.value.embeddedDialog?.let { args ->
            EmbeddedDialog(args) { result ->
                appState.update { copy(embeddedDialog = null) }
                if (result != null) handleDialogResult(result, labelerState)
            }
        }
    }
}

private fun handleDialogResult(result: EmbeddedDialogResult, labelerState: MutableState<LabelerState>) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.update { copy(canvasResolution = result.newValue) }
    }
}
