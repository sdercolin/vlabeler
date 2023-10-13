package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.dialog.AboutDialog
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialog
import com.sdercolin.vlabeler.ui.dialog.FontPreviewDialog
import com.sdercolin.vlabeler.ui.dialog.LicenseDialog
import com.sdercolin.vlabeler.ui.dialog.QuickLaunchManagerDialog
import com.sdercolin.vlabeler.ui.dialog.TrackingSettingsDialog
import com.sdercolin.vlabeler.ui.dialog.WarningDialog
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialog
import com.sdercolin.vlabeler.ui.dialog.importentries.ImportEntriesDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.MacroPluginDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.MacroPluginReportDialog
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesDialog
import com.sdercolin.vlabeler.ui.dialog.prerender.PrerenderDialog
import com.sdercolin.vlabeler.ui.dialog.project.ProjectListDialog
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialog
import com.sdercolin.vlabeler.ui.dialog.syncsample.EntrySampleSyncDialog
import com.sdercolin.vlabeler.ui.dialog.updater.UpdaterDialog
import com.sdercolin.vlabeler.ui.editor.Editor
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.Starter
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.getLocalizedMessage
import com.sdercolin.vlabeler.video.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App(
    mainScope: CoroutineScope,
    appState: AppState,
) {
    LaunchedEffect(appState) {
        appState.checkAutoSavedProject()
    }
    LaunchedEffect(appState, appState.anyDialogOpening()) {
        if (appState.anyDialogOpening()) return@LaunchedEffect
        if (appState.trackingState.hasNotAskedForTrackingPermission()) {
            appState.openTrackingSettingsDialog()
        } else if (appState.appRecordStore.value.hasCheckedRosettaCompatibleMode.not()) {
            appState.showCompatibleModeWarningIfNeeded()
        }
    }
    LaunchedEffect(appState.appConf.autoSave) {
        appState.enableAutoSaveProject(appState.appConf.autoSave, appState)
    }
    LaunchedEffect(appState) {
        appState.checkUpdates(isAuto = true)
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        when (val screen = appState.screen) {
            is Screen.Starter -> Starter(mainScope, appState)
            is Screen.ProjectCreator ->
                ProjectCreator(
                    appState = appState,
                    cancel = { appState.closeProjectCreator() },
                    activeLabelerConfs = appState.activeLabelerConfs,
                    activeTemplatePlugins = appState.getActivePlugins(Plugin.Type.Template),
                    appRecordStore = appState.appRecordStore,
                    initialFile = screen.initialFile,
                )

            is Screen.Editor -> Editor(screen.state, appState)
        }
        if (appState.isShowingProjectSettingDialog) {
            ProjectListDialog(appState, finish = { appState.closeProjectSettingDialog() })
        }
        if (appState.isShowingPrerenderDialog) {
            appState.editor?.let { editor ->
                PrerenderDialog(
                    editor.project,
                    appState.appConf,
                    editor.chartStore,
                    onError = { if (it !is CancellationException) appState.showError(it) },
                    finish = { appState.closePrerenderDialog() },
                )
            }
        }
        if (appState.isShowingEntrySampleSyncDialog) {
            EntrySampleSyncDialog(
                appState.appConf,
                appState,
                onError = { if (it !is CancellationException) appState.showError(it) },
                finish = { appState.closeEntrySampleSyncDialog() },
            )
        }
        if (appState.isShowingSampleListDialog) {
            appState.editor?.let { SampleListDialog(it, finish = { appState.closeSampleListDialog() }) }
        }
        if (appState.isShowingPreferencesDialog) {
            PreferencesDialog(appState)
        }
        if (appState.isShowingAboutDialog) {
            AboutDialog(
                appRecord = appState.appRecordFlow.value,
                appConf = appState.appConf,
                showLicenses = {
                    appState.closeAboutDialog()
                    appState.openLicenseDialog()
                },
                finish = { appState.closeAboutDialog() },
            )
        }
        if (appState.isShowingLicenseDialog) {
            LicenseDialog(appState.appConf, finish = { appState.closeLicenseDialog() })
        }
        appState.updaterDialogContent?.let { update ->
            UpdaterDialog(
                appConf = appState.appConf,
                update = update,
                appRecordStore = appState.appRecordStore,
                onError = {
                    appState.closeUpdaterDialog()
                    appState.showError(it)
                },
                finish = { appState.closeUpdaterDialog() },
            )
        }
        appState.macroPluginShownInDialog?.let { args ->
            val snackbarHostState = remember { SnackbarHostState() }
            MacroPluginDialog(
                appConf = appState.appConf,
                appRecordStore = appState.appRecordStore,
                snackbarHostState = snackbarHostState,
                args = args,
                project = appState.requireProject(),
                submit = {
                    mainScope.launch {
                        appState.closeMacroPluginDialog()
                        if (it != null) {
                            appState.showProgress()
                            withContext(Dispatchers.IO) {
                                args.plugin.saveMacroParams(
                                    it,
                                    args.plugin.getSavedParamsFile(),
                                    appState.appRecordStore,
                                    slot = args.slot,
                                )
                                appState.executeMacroPlugin(args.plugin, it, args.slot)
                            }
                            appState.hideProgress()
                        }
                    }
                },
                save = {
                    mainScope.launch(Dispatchers.IO) {
                        appState.updateMacroPluginDialogInputParams(it)
                        args.plugin.saveMacroParams(
                            it,
                            args.plugin.getSavedParamsFile(),
                            appState.appRecordStore,
                            slot = args.slot,
                        )
                    }
                },
                load = { appState.updateMacroPluginDialogInputParams(it) },
            )
        }
        appState.macroPluginReport?.let { report ->
            MacroPluginReportDialog(
                report = report,
                finish = { appState.closeMacroPluginReport() },
            )
        }
        appState.customizableItemManagerTypeShownInDialog?.let {
            CustomizableItemManagerDialog(
                it,
                appState,
            )
        }
        if (appState.isShowingQuickLaunchManagerDialog) {
            QuickLaunchManagerDialog(appState = appState)
        }
        appState.embeddedDialog?.let { request ->
            EmbeddedDialog(request)
        }
        if (appState.isShowingTrackingSettingsDialog) {
            TrackingSettingsDialog(
                appState.trackingState,
                finish = {
                    appState.closeTrackingSettingsDialog()
                    appState.trackingState.finishSettings()
                },
            )
        }
        if (appState.isShowingVideo) {
            Video(
                videoState = appState.videoState,
                playerState = appState.playerState,
                projectStore = appState,
                appConf = appState.appConf,
            )
        }
        appState.importEntriesDialogArgs?.let {
            ImportEntriesDialog(
                finish = { appState.closeImportEntriesDialog() },
                projectStore = appState,
                args = it,
            )
        }
        appState.error?.let { error ->
            WarningDialog(
                message = error.getLocalizedMessage(LocalLanguage.current),
                finish = {
                    appState.handleErrorPendingAction(appState.errorPendingAction)
                    appState.clearError()
                },
                style = WarningTextStyle.Error,
            )
        }
        if (DebugState.isShowingFontPreviewDialog) {
            FontPreviewDialog(appState.appConf, finish = { DebugState.isShowingFontPreviewDialog = false })
        }
    }
    if (appState.isBusy) {
        CircularProgress()
    }
}
