package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.DialogState
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun App(
    appConf: AppConf,
    labelerConf: LabelerConf,
    projectState: MutableState<Project?>,
    dialogState: MutableState<DialogState>,
    playerState: PlayerState,
    keyboardState: KeyboardState,
    player: Player
) {
    val labelerState = remember(appConf.painter.canvasResolution.default) {
        mutableStateOf(LabelerState(appConf.painter.canvasResolution.default))
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = projectState.value
        if (project != null) {
            Editor(
                project = project,
                playSampleSection = player::playSection,
                showDialog = { dialogState.update { copy(embedded = it) } },
                appConf = appConf,
                labelerConf = labelerConf,
                labelerState = labelerState,
                playerState = playerState,
                keyboardState = keyboardState
            )
        } else {
            Loader {
                dialogState.update { copy(openFile = true) }
            }
        }
        dialogState.value.embedded?.let { args ->
            EmbeddedDialog(args) { result ->
                dialogState.update { copy(embedded = null) }
                if (result != null) handleDialogResult(result, labelerState)
            }
        }
    }
}

@Composable
private fun Editor(
    project: Project,
    playSampleSection: (Float, Float) -> Unit,
    showDialog: (EmbeddedDialogArgs) -> Unit,
    appConf: AppConf,
    labelerConf: LabelerConf,
    labelerState: MutableState<LabelerState>,
    playerState: PlayerState,
    keyboardState: KeyboardState
) {
    val sampleState = remember { mutableStateOf<Sample?>(null) }
    val loadingState = produceState(initialValue = true, project, appConf) {
        withContext(Dispatchers.IO) {
            sampleState.value = loadSampleFile(project.currentSampleFile, appConf)
        }
        value = false
    }
    val sample = sampleState.value
    if (sample != null) {
        val localEntryState = remember {
            val entry = project.entriesBySampleName.getValue(sample.info.name)[project.currentEntryIndex]
            mutableStateOf(entry)
        }
        Labeler(
            sample = sample,
            entry = localEntryState,
            playSampleSection = playSampleSection,
            showDialog = showDialog,
            appConf = appConf,
            labelerConf = labelerConf,
            labelerState = labelerState,
            playerState = playerState,
            keyboardState = keyboardState
        )
    }
    if (loadingState.value) {
        CircularProgress()
    }
}

@Composable
private fun Loader(requestOpenFileDialog: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = requestOpenFileDialog) {
            Text("Click to load a wav file")
        }
    }
}

private fun handleDialogResult(result: EmbeddedDialogResult, labelerState: MutableState<LabelerState>) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.update { copy(canvasResolution = result.newValue) }
    }
}