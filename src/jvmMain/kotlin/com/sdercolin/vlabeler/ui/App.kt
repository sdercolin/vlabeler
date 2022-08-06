package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.io.openCreatedProject
import com.sdercolin.vlabeler.io.saveParams
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.ErrorDialog
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.MacroPluginDialog
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialog
import com.sdercolin.vlabeler.ui.editor.Editor
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App(
    mainScope: CoroutineScope,
    appState: AppState
) {
    LaunchedEffect(appState) {
        appState.checkAutoSavedProject()
    }
    LaunchedEffect(appState.appConf.autoSave) {
        appState.enableAutoSaveProject(appState.appConf.autoSave, mainScope, appState)
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
        if (appState.isShowingSampleListDialog) {
            appState.editor?.let { SampleListDialog(it, finish = { appState.closeSampleListDialog() }) }
        }
        appState.macroPluginShownInDialog?.let { (plugin, params) ->
            MacroPluginDialog(
                appRecordStore = appState.appRecordStore,
                plugin = plugin,
                paramMap = params,
                project = appState.requireProject(),
                submit = {
                    mainScope.launch {
                        appState.closeMacroPluginDialog()
                        if (it != null) {
                            appState.showProgress()
                            withContext(Dispatchers.IO) {
                                plugin.saveParams(it)
                            }
                            appState.executeMacroPlugin(plugin, it)
                            appState.hideProgress()
                        }
                    }
                },
                save = {
                    mainScope.launch(Dispatchers.IO) {
                        appState.updateMacroPluginDialogInputParams(it)
                        plugin.saveParams(it)
                    }
                }
            )
        }
        appState.customizableItemManagerTypeShownInDialog?.let {
            CustomizableItemManagerDialog(
                it,
                appState
            )
        }
        appState.embeddedDialog?.let { request ->
            EmbeddedDialog(request)
        }
        appState.error?.let { error ->
            ErrorDialog(
                error,
                finish = {
                    appState.handleErrorPendingAction(appState.errorPendingAction)
                    appState.clearError()
                }
            )
        }
    }
    if (appState.isBusy) {
        CircularProgress()
    }
}
