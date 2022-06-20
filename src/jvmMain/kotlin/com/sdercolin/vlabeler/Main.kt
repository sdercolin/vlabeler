package com.sdercolin.vlabeler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.shouldTogglePlayer
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.ProjectStateListener
import com.sdercolin.vlabeler.ui.ProjectWriter
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.DefaultAppConfFile
import com.sdercolin.vlabeler.util.getCustomLabelers
import com.sdercolin.vlabeler.util.getDefaultLabelers
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    val mainScope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf(PlayerState()) }
    val player = remember { Player(mainScope, playerState) }
    val keyboardViewModel = remember { KeyboardViewModel(mainScope) }
    val projectState = remember { mutableStateOf<Project?>(null) }
    val appState = remember { mutableStateOf(AppState()) }

    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect { event ->
            if (appState.value.isEditorActive) {
                if (event.shouldTogglePlayer) player.toggle()
            }
        }
    }

    remember { ensureDirectories() }
    val appConf = rememberAppConf(mainScope)
    val availableLabelerConfs = rememberAvailableLabelerConfs()

    Window(
        title = string(Strings.AppName),
        state = WindowState(width = 1000.dp, height = 800.dp),
        onCloseRequest = ::exitApplication,
        onKeyEvent = { keyboardViewModel.onKeyEvent(it) }
    ) {
        Menu(projectState, appState)
        AppTheme {
            App(
                mainScope,
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
        ProjectStateListener(projectState, appState)
        ProjectWriter(projectState, appState)
    }
}

@Composable
private fun rememberAppConf(scope: CoroutineScope) = remember {
    val customAppConf = if (CustomAppConfFile.exists()) {
        runCatching { parseJson<AppConf>(CustomAppConfFile.readText()) }.getOrNull()
    } else null
    val appConf = customAppConf ?: parseJson(DefaultAppConfFile.readText())
    scope.launch(Dispatchers.IO) {
        CustomAppConfFile.writeText(toJson(appConf))
    }
    mutableStateOf(appConf)
}

@Composable
private fun rememberAvailableLabelerConfs() = remember {
    val defaultLabelers = getDefaultLabelers()
    val customLabelers = getCustomLabelers()
    val customLabelerNames = customLabelers.map { it.name }
    defaultLabelers.filter { it.name !in customLabelerNames }.forEach {
        it.copyTo(CustomLabelerDir.resolve(it.name))
    }

    val availableLabelerConfs = getCustomLabelers().map { it.readText() }.map { parseJson<LabelerConf>(it) }
    if (availableLabelerConfs.isEmpty()) {
        throw Exception("No labeler configuration files found.")
    }
    mutableStateOf(availableLabelerConfs)
}

private fun ensureDirectories() {
    if (AppDir.exists().not()) AppDir.mkdir()
    if (CustomLabelerDir.exists().not()) CustomLabelerDir.mkdir()
}
