package com.sdercolin.vlabeler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.ProjectChangesListener
import com.sdercolin.vlabeler.ui.ProjectWriter
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.labeler.ScrollFitViewModel
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
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() = application {
    val mainScope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf(PlayerState()) }
    val player = remember { Player(mainScope, playerState) }
    val keyboardViewModel = remember { KeyboardViewModel(mainScope) }
    val scrollFitViewModel = remember { ScrollFitViewModel(mainScope) }
    val appState = remember { mutableStateOf(AppState()) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        onCloseRequest = { appState.update { requestExit() } },
        onKeyEvent = { keyboardViewModel.onKeyEvent(it) }
    ) {
        Menu(appState, scrollFitViewModel, snackbarHostState)
        AppTheme {
            App(
                mainScope,
                appConf.value,
                availableLabelerConfs.value,
                appState,
                playerState.value,
                snackbarHostState,
                keyboardViewModel,
                scrollFitViewModel,
                player
            )
        }
        StandaloneDialogs(availableLabelerConfs.value, appState)
        ProjectChangesListener(appState)
        ProjectWriter(appState)
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
    LaunchExit(appState, ::exitApplication)
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
    val defaultLabelers = getDefaultLabelers().associateWith {
        it.asLabelerConf().getOrThrow() // default items should always be parsed
    }.toList()
    val defaultLabelerNames = defaultLabelers.map { it.first.name }
    val customLabelers = getCustomLabelers().associateWith {
        it.asLabelerConf().getOrElse {
            // TODO: log
            null
        }
    }.toList()
    val validCustomLabelers = customLabelers.mapNotNull { (file, result) -> result?.let { file to it } }
    val validCustomLabelerNames = validCustomLabelers.map { it.first.name }

    val availableLabelers = mutableListOf<LabelerConf>()
    val (duplicated, new) = defaultLabelers.partition { it.first.name in validCustomLabelerNames }
    new.forEach {
        availableLabelers.add(it.second)
        it.first.copyTo(CustomLabelerDir.resolve(it.first.name))
    }
    duplicated.forEach { default ->
        val custom = validCustomLabelers.first { it.first.name == default.first.name }
        if (default.second.version > custom.second.version) {
            // update with default
            availableLabelers.add(default.second)
            default.first.copyTo(CustomLabelerDir.resolve(custom.first.name), overwrite = true)
        } else {
            availableLabelers.add(custom.second)
        }
    }
    availableLabelers.addAll(
        validCustomLabelers.filter { it.first.name !in defaultLabelerNames }.map { it.second }
    )
    if (availableLabelers.isEmpty()) {
        throw Exception("No labeler configuration files found.")
    }
    mutableStateOf(availableLabelers)
}

private fun File.asLabelerConf() = runCatching {
    parseJson<LabelerConf>(this@asLabelerConf.readText()).copy(
        name = name.removeSuffix(".${LabelerConf.LabelerFileExtension}")
    )
}

private fun ensureDirectories() {
    if (AppDir.exists().not()) AppDir.mkdir()
    if (CustomLabelerDir.exists().not()) CustomLabelerDir.mkdir()
}

@Composable
private fun LaunchExit(appState: State<AppState>, exit: () -> Unit) {
    val shouldExit = appState.value.shouldExit
    LaunchedEffect(shouldExit) {
        if (shouldExit) exit()
    }
}
