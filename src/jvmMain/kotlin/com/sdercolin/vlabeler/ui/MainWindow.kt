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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.io.loadSampleFile
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.FileDialog
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.labeler.LabelerState
import com.sdercolin.vlabeler.util.update
import java.io.File

@Composable
fun MainWindow(
    player: Player,
    appConf: MutableState<AppConf>,
    labelerConf: MutableState<LabelerConf>,
    playerState: PlayerState,
    keyboardState: KeyboardState
) {
    val sampleState = remember { mutableStateOf<Sample?>(null) }
    val dialogState = remember { mutableStateOf<EmbeddedDialogArgs?>(null) }
    val labelerState = remember(appConf.value.painter.canvasResolution.default) {
        mutableStateOf(LabelerState(appConf.value.painter.canvasResolution.default))
    }

    val sample = sampleState.value
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        if (sample != null) {
            Labeler(
                sample = sample,
                playSampleSection = player::playSection,
                showDialog = { dialogState.update { it } },
                appConf = appConf.value,
                labelerConf = labelerConf.value,
                labelerState = labelerState,
                playerState = playerState,
                keyboardState = keyboardState
            )
        } else {
            Loader(appConf.value) {
                sampleState.value = it
                player.load(it.info.file)
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
private fun Loader(appConf: AppConf, onLoaded: (Sample) -> Unit) {
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
                        val sample = loadSampleFile(file, appConf)
                        onLoaded(sample)
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