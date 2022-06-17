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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.FileDialog
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainWindow(
    player: Player,
    appConf: MutableState<AppConf>,
    labelerConf: MutableState<LabelerConf>,
    playerState: PlayerState,
    keyboardState: KeyboardState
) {
    val projectState = remember { mutableStateOf<Project?>(null) }
    val dialogState = remember { mutableStateOf<EmbeddedDialogArgs?>(null) }
    val labelerState = remember(appConf.value.painter.canvasResolution.default) {
        mutableStateOf(LabelerState(appConf.value.painter.canvasResolution.default))
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        val project = projectState.value
        if (project != null) {
            Editor(
                project = project,
                playSampleSection = player::playSection,
                showDialog = { dialogState.update { it } },
                appConf = appConf.value,
                labelerConf = labelerConf.value,
                labelerState = labelerState,
                playerState = playerState,
                keyboardState = keyboardState
            )
        } else {
            Loader(appConf.value, labelerConf.value) {
                projectState.value = it
            }
        }
        dialogState.value?.let { args ->
            EmbeddedDialog(args) { result ->
                dialogState.update { null }
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
private fun Loader(appConf: AppConf, labelerConf: LabelerConf, onLoaded: (Project) -> Unit) {
    var isFileDialogOpened by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = { isFileDialogOpened = true }) {
            Text("Click to load a wav file")
        }
        if (isFileDialogOpened) {
            FileDialog(
                onCloseRequest = { directory, fileName ->
                    isFileDialogOpened = false
                    if (directory != null && fileName != null) {
                        val file = File(directory, fileName)
                        val project = Project(
                            workingDirectory = File(directory),
                            entriesBySampleName = mapOf(
                                file.nameWithoutExtension to listOf(
                                    Entry("i あ", 2615f, 3315f, listOf(3055f, 2915f, 2715f))
                                )
                            ),
                            appConf,
                            labelerConf,
                            currentSampleName = file.nameWithoutExtension,
                            currentEntryIndex = 0
                        )
                        onLoaded(project)
                    }
                }
            )
        }
    }
}

private fun handleDialogResult(result: EmbeddedDialogResult, labelerState: MutableState<LabelerState>) {
    when (result) {
        is SetResolutionDialogResult -> labelerState.update { copy(canvasResolution = result.newValue) }
    }
}