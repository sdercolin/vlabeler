package com.sdercolin.vlabeler

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyEventHandler
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.dialog.DialogState
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

fun main() = application {
    val mainScope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf(PlayerState()) }
    val player = remember { Player(mainScope, playerState) }
    val keyboardState = remember { mutableStateOf(KeyboardState()) }
    val keyEventHandler = remember { KeyEventHandler(player, keyboardState) }
    val projectState = remember { mutableStateOf<Project?>(null) }
    val dialogState = remember { mutableStateOf(DialogState()) }

    val resourcesDir = remember { File(System.getProperty("compose.application.resources.dir")) }
    val appConf = remember {
        resourcesDir.resolve("app.conf.json").readText()
            .let { Json.decodeFromString<AppConf>(it) }
            .let { mutableStateOf(it) }
    }
    val labelerConf = remember {
        resourcesDir.resolve("oto.labeler.json").readText()
            .let { Json.decodeFromString<LabelerConf>(it) }
            .let { conf -> conf.copy(fields = conf.fields.sortedBy { it.index }) }
            .let { mutableStateOf(it) }
    }

    Window(title = "vlabeler", onCloseRequest = ::exitApplication, onKeyEvent = { keyEventHandler.onKeyEvent(it) }) {
        Menu(projectState, dialogState)
        AppTheme {
            App(
                appConf.value,
                labelerConf.value,
                projectState,
                dialogState,
                playerState.value,
                keyboardState.value,
                player
            )
        }
        StandaloneDialogs(appConf.value, labelerConf.value, projectState, dialogState)
    }
}
