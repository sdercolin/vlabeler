package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.io.openCreatedProject
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialog
import com.sdercolin.vlabeler.ui.editor.Editor
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(
    mainScope: CoroutineScope,
    appState: AppState
) {
    LaunchedEffect(appState) {
        appState.checkAutoSavedProject()
    }
    LaunchedEffect(appState.appConf.autoSaveIntervalSecond) {
        appState.enableAutoSaveProject(appState.appConf.autoSaveIntervalSecond, mainScope, appState)
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        when (val screen = appState.screen) {
            is Screen.Starter -> Starter(mainScope, appState)
            is Screen.ProjectCreator ->
                ProjectCreator(
                    create = { openCreatedProject(mainScope, it, appState) },
                    cancel = { appState.closeProjectCreator() },
                    availableLabelerConfs = appState.availableLabelerConfs,
                    availableTemplatePlugins = appState.getPlugins(Plugin.Type.Template),
                    snackbarHostState = appState.snackbarHostState,
                    appRecordStore = appState.appRecordStore
                )
            is Screen.Editor -> Editor(screen.state, appState)
        }
        appState.embeddedDialog?.let { request ->
            EmbeddedDialog(request)
        }
        if (appState.isShowingSampleListDialog) {
            appState.editor?.let { SampleListDialog(it, finish = { appState.closeSampleListDialog() }) }
        }
    }
    if (appState.isBusy) {
        CircularProgress()
    }
}
