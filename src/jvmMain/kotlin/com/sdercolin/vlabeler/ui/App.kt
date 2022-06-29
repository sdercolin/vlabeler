package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.io.openCreatedProject
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.editor.Editor
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(
    mainScope: CoroutineScope,
    appState: AppState
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        when (val screen = appState.screen) {
            is AppState.Screen.Starter -> Starter(mainScope, appState)
            is AppState.Screen.ProjectCreator ->
                ProjectCreator(
                    create = { openCreatedProject(mainScope, it, appState) },
                    cancel = { appState.closeProjectCreator() },
                    availableLabelerConfs = appState.availableLabelerConfs,
                    snackbarHostState = appState.snackbarHostState
                )
            is AppState.Screen.Editor -> Editor(screen.editorState, appState)
        }
        appState.embeddedDialog?.let { args ->
            EmbeddedDialog(args) { result ->
                appState.closeEmbeddedDialog()
                if (result != null) appState.handleDialogResult(result)
            }
        }
    }
    if (appState.isBusy) {
        CircularProgress()
    }
}
