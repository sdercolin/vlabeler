@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

fun main() = application {
    val mainScope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf(PlayerState()) }
    val player = remember { Player(mainScope, playerState) }
    val keyboardViewModel = remember { KeyboardViewModel(mainScope) }
    val projectState = remember { mutableStateOf<Project?>(null) }
    val appState = remember { mutableStateOf(AppState()) }

    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect { event ->
            if (event.key == Key.Spacebar && event.type == KeyEventType.KeyUp) {
                player.toggle()
            }
        }
    }

    val resourcesDir = remember { File(System.getProperty("compose.application.resources.dir")) }
    val appConf = remember {
        resourcesDir.resolve("app.conf.json").readText()
            .let { Json.decodeFromString<AppConf>(it) }
            .let { mutableStateOf(it) }
    }
    val availableLabelerConfs = remember {
        resourcesDir.resolve("labelers").listFiles().orEmpty()
            .filter { it.name.endsWith(".labeler.json") }
            .map { it.readText() }
            .map { Json.decodeFromString<LabelerConf>(it) }
            .let { mutableStateOf(it) }
    }
    if (availableLabelerConfs.value.isEmpty()) {
        throw Exception("No labeler configuration files found.")
    }

    Window(
        title = string(Strings.AppName),
        state = WindowState(width = 1000.dp, height = 800.dp),
        onCloseRequest = ::exitApplication,
        onKeyEvent = { keyboardViewModel.onKeyEvent(it) }
    ) {
        Menu(projectState, appState)
        AppTheme {
            App(
                appConf.value,
                availableLabelerConfs.value,
                projectState,
                appState,
                playerState.value,
                keyboardViewModel,
                player
            )
        }
        StandaloneDialogs(availableLabelerConfs.value, projectState, appState)
    }
}
