package com.sdercolin.vlabeler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sdercolin.vlabeler.audio.Player
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Locale
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.env.osInfo
import com.sdercolin.vlabeler.env.osName
import com.sdercolin.vlabeler.env.runtimeVersion
import com.sdercolin.vlabeler.io.ensureDirectories
import com.sdercolin.vlabeler.io.initializeGlobalRepositories
import com.sdercolin.vlabeler.io.loadAppConf
import com.sdercolin.vlabeler.io.produceAppState
import com.sdercolin.vlabeler.io.runMigration
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.parseArgs
import com.sdercolin.vlabeler.tracking.event.LaunchEvent
import com.sdercolin.vlabeler.ui.App
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Menu
import com.sdercolin.vlabeler.ui.ProjectChangesListener
import com.sdercolin.vlabeler.ui.ProjectWriter
import com.sdercolin.vlabeler.ui.Splash
import com.sdercolin.vlabeler.ui.dialog.StandaloneDialogs
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.AppRecordFile
import com.sdercolin.vlabeler.util.MemoryUsageMonitor
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

var hasUncaughtError = false

val UseCustomFileDialog = compositionLocalOf { false }

fun main(vararg args: String) = application {
    remember { Log.init() }
    remember { ensureDirectories() }

    val mainScope = rememberCoroutineScope()
    val appRecordStore = rememberAppRecordStore(mainScope)
    remember { initializeGlobalRepositories(appRecordStore) }
    remember { runMigration(appRecordStore) }

    val appRecord = appRecordStore.stateFlow.collectAsState()
    val windowState = rememberResizableWindowState(appRecord)
    val appConf = remember { loadAppConf(mainScope, appRecordStore) }

    currentLanguage = appConf.value.view.language

    val appState by produceState(null as AppState?) {
        value = produceAppState(mainScope, appConf, appRecordStore, parseArgs(args.toList()))
    }
    val onCloseRequest = {
        if (hasUncaughtError) {
            runCatching { appState?.exit(fromError = true) }
            exitApplication()
        } else {
            appState?.requestExit() ?: exitApplication()
        }
    }
    val onKeyEvent: (KeyEvent) -> Boolean = {
        appState?.keyboardViewModel?.onKeyEvent(it) ?: false
    }

    LaunchedEffect(DebugState.printMemoryUsage) {
        if (DebugState.printMemoryUsage) {
            MemoryUsageMonitor().run()
        }
    }

    val windowTitle = string(Strings.AppName) + appState?.project?.projectName?.let { " - $it" }.orEmpty()

    CompositionLocalProvider(UseCustomFileDialog.provides(appConf.value.misc.useCustomFileDialog)) {
        Window(
            title = windowTitle,
            icon = painterResource(Resources.iconIco),
            state = windowState,
            onCloseRequest = onCloseRequest,
            onKeyEvent = onKeyEvent,
        ) {
            LaunchWindowFocusListener(appState)
            LaunchSaveWindowSize(windowState, appRecordStore)
            Menu(mainScope, appState, appConf.value.view)

            if (appState == null) {
                AppTheme(appConf.value.view) { Splash() }
            }

            appState?.let { state ->
                LaunchValidate(state)
                LaunchKeyboardEvent(state.keyboardViewModel, state, state.player)
                LaunchExit(state, ::exitApplication)
                LaunchTrackingLaunch(state)
                AppTheme(state.appConf.view) { App(mainScope, state) }
                StandaloneDialogs(mainScope, state)
                ProjectChangesListener(state)
                ProjectWriter(state)
                SnackbarBox(state)
            }
        }
    }
}

@Composable
private fun SnackbarBox(state: AppState) {
    AppTheme(state.appConf.view) {
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(
                state.snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Snackbar(
                    it,
                    actionColor = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
private fun rememberAppRecordStore(scope: CoroutineScope) = remember {
    val recordText = AppRecordFile.takeIf { it.exists() }?.readText()
    val appRecord = runCatching { recordText?.parseJson<AppRecord>() }.getOrNull() ?: AppRecord()
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
private fun LaunchValidate(state: AppState) {
    LaunchedEffect(state) {
        state.validate()
    }
}

@Composable
private fun LaunchSaveWindowSize(
    windowState: WindowState,
    appRecordStore: AppRecordStore,
) {
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.size }
            .onEach(appRecordStore::saveWindowSize)
            .launchIn(this)
    }
}

@Composable
private fun WindowScope.LaunchWindowFocusListener(appState: AppState?) {
    var added by remember { mutableStateOf(false) }
    DisposableEffect(appState) {
        if (appState != null && !added) {
            added = true
            window.addWindowFocusListener(
                object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent) {
                    }

                    override fun windowLostFocus(e: WindowEvent) {
                        appState.keyboardViewModel.clear()
                    }
                },
            )
        }
        onDispose {}
    }
}

@Composable
private fun LaunchKeyboardEvent(
    keyboardViewModel: KeyboardViewModel,
    appState: AppState,
    player: Player,
) {
    LaunchedEffect(appState, keyboardViewModel, player) {
        keyboardViewModel.keyboardActionFlow.collect { action ->
            if (action == KeyAction.CancelDialog) {
                appState.closeAllDialogs()
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

@Composable
private fun LaunchTrackingLaunch(appState: AppState) {
    LaunchedEffect(appState) {
        appState.track(
            LaunchEvent(
                appVersion = appVersion.toString(),
                runtime = runtimeVersion?.toString().orEmpty(),
                osName = osName,
                osInfo = osInfo,
                isDebug = isDebug,
                locale = Locale.toString(),
            ),
        )
    }
}
