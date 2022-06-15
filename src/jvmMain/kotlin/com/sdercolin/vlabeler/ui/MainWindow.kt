package com.sdercolin.vlabeler.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.sdercolin.vlabeler.ui.env.FileDialog
import com.sdercolin.vlabeler.ui.labeler.Labeler
import com.sdercolin.vlabeler.ui.theme.AppTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope

@Composable
fun MainWindow(
    mainScope: CoroutineScope,
    appConf: AppConf,
    labelerConf: LabelerConf,
    player: Player,
    playerState: PlayerState,
    keyboardState: KeyboardState
) {
    val sampleState = remember { mutableStateOf<Sample?>(null) }

    AppTheme {
        val sample = sampleState.value
        if (sample != null) {
            Labeler(
                sample = sample,
                appConf = appConf,
                labelerConf = labelerConf,
                playerState = playerState,
                playSampleSection = player::playSection,
                keyboardState = keyboardState
            )
        } else {
            Loader(appConf) {
                sampleState.value = it
                player.load(it.info.file)
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
        Button(onClick = { isFileDialogOpened = true }) {
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