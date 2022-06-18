package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.shouldGoNextEntry
import com.sdercolin.vlabeler.env.shouldGoNextSample
import com.sdercolin.vlabeler.env.shouldGoPreviousEntry
import com.sdercolin.vlabeler.env.shouldGoPreviousSample
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun App(
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
                showDialog = { appState.update { copy(embeddedDialog = it) } },
                appConf = appConf,
                labelerState = labelerState,
                playerState = playerState,
                keyboardViewModel = keyboardViewModel,
                player = player
            )
        } else {
            Starter(
                appState = appState,
                requestNewProject = {
                    saveProjectFile(it)
                    appState.update { copy(isConfiguringNewProject = false) }
                    projectState.update { it }
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

@Composable
private fun Editor(
    project: Project,
    showDialog: (EmbeddedDialogArgs) -> Unit,
    appConf: AppConf,
    labelerState: MutableState<LabelerState>,
    playerState: PlayerState,
    keyboardViewModel: KeyboardViewModel,
    player: Player
) {
    val sampleState = produceState(initialValue = null as Sample?, project.currentSampleName, appConf) {
        value = withContext(Dispatchers.IO) {
            loadSampleFile(project.currentSampleFile, appConf)
        }
    }
    val isLoading by remember { derivedStateOf { sampleState.value == null } }
    val localEntryState = remember { mutableStateOf(project.currentEntry) }
    LaunchedEffect(project.currentEntry) {
        // TODO: post local state upward
        localEntryState.value = project.currentEntry
    }

    val sample = sampleState.value
    LaunchedEffect(sample) {
        if (sample != null) player.load(sample.info.file)
    }
    Labeler(
        sample = sample,
        sampleName = project.currentSampleName,
        entry = localEntryState.value,
        currentEntryIndexInTotal = project.currentEntryIndexInTotal,
        totalEntryCount = project.totalEntryCount,
        editEntry = { localEntryState.update { it } },
        playSampleSection = player::playSection,
        showDialog = showDialog,
        appConf = appConf,
        labelerConf = project.labelerConf,
        labelerState = labelerState,
        playerState = playerState,
        keyboardViewModel = keyboardViewModel
    )
    if (isLoading) {
        CircularProgress()
    }
}

private fun handleDialogResult(result: EmbeddedDialogResult, labelerState: MutableState<LabelerState>) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.update { copy(canvasResolution = result.newValue) }
    }
}

private fun saveProjectFile(project: Project) {
    val workingDirectory = File(project.workingDirectory)
    if (!workingDirectory.exists()) {
        workingDirectory.mkdir()
    }
    val projectContent = Json.encodeToString(project)
    project.projectFile.writeText(projectContent)
}
