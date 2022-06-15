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
import com.sdercolin.vlabeler.ui.MainWindow
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun main() = application {
    val mainScope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf(PlayerState()) }
    val player = remember { Player(mainScope, playerState) }
    val keyboardState = remember { mutableStateOf(KeyboardState()) }
    val keyEventHandler = KeyEventHandler(player, keyboardState)

    val resourcesDir = File(System.getProperty("compose.application.resources.dir"))

    val appConf = resourcesDir.resolve("app.conf.json").readText().let {
        Json.decodeFromString<AppConf>(it)
    }
    val labelerConf = resourcesDir.resolve("oto.labeler.json").readText().let {
        Json.decodeFromString<LabelerConf>(it)
    }.let { conf -> conf.copy(fields = conf.fields.sortedBy { it.index }) }

    Window(title = "vlabeler", onCloseRequest = ::exitApplication, onKeyEvent = { keyEventHandler.onKeyEvent(it) }) {
        MainWindow(mainScope, appConf, labelerConf, player, playerState.value, keyboardState.value)
    }
}

