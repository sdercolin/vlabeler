package com.sdercolin.vlabeler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.shouldTogglePlayer
import com.sdercolin.vlabeler.io.ensureDirectories
import com.sdercolin.vlabeler.io.produceAppState
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.ProjectChangesListener
import com.sdercolin.vlabeler.ui.ProjectWriter
import com.sdercolin.vlabeler.ui.Splash
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.AppRecordFile
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun main() = application {
    Log.init()
    ensureDirectories()

    val mainScope = rememberCoroutineScope()
    val appRecordStore = rememberAppRecordStore(mainScope)

    val appRecord = appRecordStore.stateFlow.collectAsState()
    val windowState = rememberResizableWindowState(appRecord)

    val appState by produceState(null as AppState?) {
        value = produceAppState(mainScope, appRecordStore)
    }
    val onCloseRequest = {
        appState?.requestExit() ?: exitApplication()
    }
    val onKeyEvent: (KeyEvent) -> Boolean = {
        appState?.keyboardViewModel?.onKeyEvent(it) ?: false
    }

    Window(
        title = string(Strings.AppName),
        icon = painterResource("icon.ico"),
        state = windowState,
        onCloseRequest = onCloseRequest,
        onKeyEvent = onKeyEvent
    ) {
        LaunchSaveWindowSize(windowState, appRecordStore)
        if (appState == null) {
            AppTheme { Splash() }
        }

        appState?.let { state ->
            LaunchKeyboardEvent(state.keyboardViewModel, state, state.player)
            LaunchExit(state, ::exitApplication)

            Menu(mainScope, state)
            AppTheme { App(mainScope, state) }
            StandaloneDialogs(mainScope, state)
            ProjectChangesListener(state)
            ProjectWriter(state)
            Box(Modifier.fillMaxSize()) {
                SnackbarHost(state.snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun rememberAppRecordStore(scope: CoroutineScope) = remember {
    val recordText = AppRecordFile.takeIf { it.exists() }?.readText()
    val appRecord = recordText?.let { parseJson(it) } ?: AppRecord()
    AppRecordStore(appRecord, scope)
}

@Composable
private fun rememberResizableWindowState(appRecord: State<AppRecord>): WindowState {
    val windowSize = remember { appRecord.value.windowSizeDp }
    return rememberWindowState(width = windowSize.first.dp, height = windowSize.second.dp)
}

private fun AppRecordStore.saveWindowSize(dpSize: DpSize) {
    val size = dpSize.width.value to dpSize.height.value
    Log.info("Window size changed: $size")
    update { copy(windowSizeDp = size) }
}

@Composable
private fun LaunchSaveWindowSize(
    windowState: WindowState,
    appRecordStore: AppRecordStore
) {
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.size }
            .onEach(appRecordStore::saveWindowSize)
            .launchIn(this)
    }
}

@Composable
private fun LaunchKeyboardEvent(
    keyboardViewModel: KeyboardViewModel,
    appState: AppState,
    player: Player
) {
    LaunchedEffect(Unit) {
        keyboardViewModel.keyboardEventFlow.collect { event ->
            if (appState.isEditorActive) {
                if (event.shouldTogglePlayer) player.toggle()
            }
        }
    }
}

@Composable
private fun LaunchExit(appState: AppState, exit: () -> Unit) {
    val shouldExit = appState.shouldExit
    LaunchedEffect(shouldExit) {
        if (shouldExit) exit()
    }
}
