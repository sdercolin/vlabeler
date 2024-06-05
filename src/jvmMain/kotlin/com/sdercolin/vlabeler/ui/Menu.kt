package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.io.install
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.Clipboard
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.getNullableOrElse
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File

@Composable
fun FrameWindowScope.Menu(
    mainScope: CoroutineScope,
    appState: AppState?,
    viewConf: AppConf.View,
) {
    var errorForDebug by remember { mutableStateOf<Throwable?>(null) }
    val keymap = appState?.appConf?.keymaps?.keyActionMap ?: mapOf()

    fun KeyAction.getKeyShortCut() = keymap.getNullableOrElse(this) { this.defaultKeySet }?.toShortCut()

    LaunchedEffect(errorForDebug) {
        errorForDebug?.let {
            throw it
        }
    }

    CompositionLocalProvider(LocalLanguage.provides(viewConf.language)) {
        MenuBar {
            Menu(string(Strings.MenuFile), mnemonic = 'F') {
                if (appState != null) {
                    Item(
                        string(Strings.MenuFileNewProject),
                        onClick = { appState.requestOpenProjectCreator() },
                        shortcut = KeyAction.NewProject.getKeyShortCut(),
                    )
                    Item(
                        string(Strings.MenuFileOpen),
                        onClick = { appState.requestOpenProject() },
                        shortcut = KeyAction.OpenProject.getKeyShortCut(),
                    )
                    val appRecord by appState.appRecordFlow.collectAsState()
                    Menu(string(Strings.MenuFileOpenRecent)) {
                        appRecord.recentProjectPathsWithDisplayNames.forEach { (path, displayName) ->
                            Item(
                                text = displayName,
                                onClick = { appState.requestOpenCertainProject(mainScope, File(path)) },
                            )
                        }
                        Separator()
                        Item(
                            text = string(Strings.MenuFileOpenRecentClear),
                            onClick = { appState.clearRecentProjects() },
                            shortcut = KeyAction.ClearRecentProjects.getKeyShortCut(),
                            enabled = appRecord.recentProjects.isNotEmpty(),
                        )
                    }
                    Item(
                        string(Strings.MenuFileSave),
                        onClick = { appState.requestSave() },
                        shortcut = KeyAction.SaveProject.getKeyShortCut(),
                        enabled = appState.hasUnsavedChanges,
                    )
                    Item(
                        string(Strings.MenuFileSaveAs),
                        onClick = { appState.openSaveAsProjectDialog() },
                        shortcut = KeyAction.SaveProjectAs.getKeyShortCut(),
                        enabled = appState.hasProject,
                    )
                    Item(
                        string(Strings.MenuFileProjectSetting),
                        onClick = { appState.openProjectSettingDialog() },
                        shortcut = KeyAction.OpenProjectSetting.getKeyShortCut(),
                        enabled = appState.hasProject,
                    )
                    Item(
                        string(Strings.MenuFileImport),
                        onClick = { appState.openImportDialog() },
                        shortcut = KeyAction.ImportProject.getKeyShortCut(),
                        enabled = appState.hasProject,
                    )
                    Item(
                        string(Strings.MenuFileExport),
                        onClick = { appState.requestExport(overwrite = false) },
                        shortcut = KeyAction.ExportProject.getKeyShortCut(),
                        enabled = appState.hasProject,
                    )
                    Item(
                        string(Strings.MenuFileExportOverwrite),
                        onClick = { appState.requestExport(overwrite = true) },
                        shortcut = KeyAction.ExportProjectOverwrite.getKeyShortCut(),
                        enabled = appState.hasProject && appState.canOverwriteExportCurrentModule(),
                    )
                    if (appState.shouldShowOverwriteExportAllModules()) {
                        Item(
                            string(Strings.MenuFileExportOverwriteAll),
                            onClick = { appState.requestExport(overwrite = true, all = true) },
                            shortcut = KeyAction.ExportProjectOverwriteAll.getKeyShortCut(),
                            enabled = appState.hasProject && appState.canOverwriteExportAllModules(),
                        )
                    }
                    Item(
                        string(Strings.MenuFileInvalidateCaches),
                        onClick = { appState.requestClearCaches(mainScope) },
                        shortcut = KeyAction.InvalidateCaches.getKeyShortCut(),
                        enabled = appState.hasProject && appState.isShowingPrerenderDialog.not(),
                    )
                    Item(
                        string(Strings.MenuFileClose),
                        onClick = { appState.requestCloseProject() },
                        shortcut = KeyAction.CloseProject.getKeyShortCut(),
                        enabled = appState.hasProject,
                    )
                }
            }
            Menu(string(Strings.MenuEdit), mnemonic = 'E') {
                if (appState != null) {
                    Item(
                        string(Strings.MenuEditUndo),
                        onClick = { appState.undo() },
                        shortcut = KeyAction.Undo.getKeyShortCut(),
                        enabled = appState.history.canUndo,
                    )
                    Item(
                        string(Strings.MenuEditRedo),
                        onClick = { appState.redo() },
                        shortcut = KeyAction.Redo.getKeyShortCut(),
                        enabled = appState.history.canRedo,
                    )
                    Menu(string(Strings.MenuEditTools)) {
                        Tool.values().forEach { tool ->
                            CheckboxItem(
                                string(tool.stringKey),
                                checked = appState.editor?.let { it.tool == tool } ?: false,
                                onCheckedChange = { if (it) appState.editor?.tool = tool },
                                shortcut = tool.keyAction.getKeyShortCut(),
                                enabled = appState.isEditorActive,
                            )
                        }
                    }
                    Item(
                        string(Strings.MenuEditRenameEntry),
                        onClick = {
                            appState.openEditEntryNameDialog(
                                index = appState.requireProject().currentModule.currentIndex,
                                purpose = InputEntryNameDialogPurpose.Rename,
                            )
                        },
                        shortcut = KeyAction.RenameCurrentEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditDuplicateEntry),
                        onClick = {
                            appState.openEditEntryNameDialog(
                                index = appState.requireProject().currentModule.currentIndex,
                                purpose = InputEntryNameDialogPurpose.Duplicate,
                            )
                        },
                        shortcut = KeyAction.DuplicateCurrentEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditRemoveEntry),
                        onClick = { appState.confirmIfRemoveCurrentEntry(appState.isCurrentEntryTheLast()) },
                        shortcut = KeyAction.RemoveCurrentEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditMoveEntry),
                        onClick = { appState.openMoveCurrentEntryDialog(appState.appConf) },
                        shortcut = KeyAction.MoveCurrentEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canMoveEntry,
                    )
                    Item(
                        string(Strings.MenuEditEditTag),
                        onClick = { appState.editor?.isEditingTag = true },
                        shortcut = KeyAction.EditTag.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditToggleDone),
                        onClick = { appState.toggleCurrentEntryDone() },
                        shortcut = KeyAction.ToggleDone.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditToggleStar),
                        onClick = { appState.toggleCurrentEntryStar() },
                        shortcut = KeyAction.ToggleStar.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuEditEditEntryExtra),
                        onClick = {
                            appState.openEditEntryExtraDialog(
                                index = appState.requireProject().currentModule.currentIndex,
                            )
                        },
                        shortcut = KeyAction.EditEntryExtra.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canEditCurrentEntryExtra,
                    )
                    CheckboxItem(
                        string(Strings.MenuEditMultipleEditMode),
                        checked = appState.project?.multipleEditMode == true,
                        onCheckedChange = { appState.toggleMultipleEditMode(it) },
                        shortcut = KeyAction.ToggleMultipleEditMode.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.project?.labelerConf?.continuous == true,
                    )
                    Item(
                        string(Strings.MenuEditEditModuleExtra),
                        onClick = {
                            appState.openEditModuleExtraDialog()
                        },
                        shortcut = KeyAction.EditModuleExtra.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canEditCurrentModuleExtra,
                    )
                }
            }
            Menu(string(Strings.MenuView), mnemonic = 'V') {
                if (appState != null) {
                    CheckboxItem(
                        string(Strings.MenuViewToggleMarker),
                        checked = appState.isMarkerDisplayed,
                        onCheckedChange = { appState.isMarkerDisplayed = it },
                        shortcut = KeyAction.ToggleMarker.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    CheckboxItem(
                        string(Strings.MenuViewToggleProperties),
                        checked = appState.isPropertyViewDisplayed,
                        onCheckedChange = { appState.isPropertyViewDisplayed = it },
                        shortcut = KeyAction.ToggleProperties.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    CheckboxItem(
                        string(Strings.MenuViewPinEntryList),
                        checked = appState.isEntryListPinned,
                        onCheckedChange = { appState.isEntryListPinned = it },
                        shortcut = KeyAction.TogglePinnedEntryList.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    CheckboxItem(
                        string(Strings.MenuViewPinEntryListLocked),
                        checked = appState.pinnedEntryListSplitPanePositionLocked,
                        onCheckedChange = { appState.pinnedEntryListSplitPanePositionLocked = it },
                        shortcut = KeyAction.TogglePinnedEntryListLocked.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    CheckboxItem(
                        string(Strings.MenuViewToggleToolbox),
                        checked = appState.isToolboxDisplayed,
                        onCheckedChange = { appState.isToolboxDisplayed = it },
                        shortcut = KeyAction.ToggleToolbox.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    CheckboxItem(
                        string(Strings.MenuViewToggleTimescaleBar),
                        checked = appState.isTimescaleBarDisplayed,
                        onCheckedChange = { appState.isTimescaleBarDisplayed = it },
                        shortcut = KeyAction.ToggleTimescaleBar.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuViewOpenSampleList),
                        onClick = { appState.openSampleListDialog() },
                        shortcut = KeyAction.OpenSampleList.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Menu(string(Strings.MenuViewVideo)) {
                        CheckboxItem(
                            string(Strings.MenuViewVideoOff),
                            checked = !appState.isShowingVideo,
                            onCheckedChange = { if (it) appState.toggleVideoPopup(false) },
                            enabled = appState.isEditorActive,
                        )
                        CheckboxItem(
                            string(Strings.MenuViewVideoEmbedded),
                            checked = appState.isShowingVideo && appState.videoState.isEmbeddedMode,
                            onCheckedChange = {
                                appState.videoState.setEmbeddedMode()
                                appState.toggleVideoPopup(it)
                            },
                            shortcut = KeyAction.ToggleVideoPopupEmbedded.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                        CheckboxItem(
                            string(Strings.MenuViewVideoNewWindow),
                            checked = appState.isShowingVideo && appState.videoState.isNewWindowMode,
                            onCheckedChange = {
                                appState.videoState.setNewWindowMode()
                                appState.toggleVideoPopup(it)
                            },
                            shortcut = KeyAction.ToggleVideoPopupNewWindow.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                    }
                }
            }
            Menu(string(Strings.MenuNavigate), mnemonic = 'N') {
                if (appState != null) {
                    Menu(string(Strings.MenuNavigateOpenLocation)) {
                        Item(
                            string(Strings.MenuNavigateOpenLocationRootDirectory),
                            onClick = { appState.openRootDirectory() },
                            shortcut = KeyAction.NavigateOpenRootDirectory.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                        Item(
                            string(Strings.MenuNavigateOpenLocationModuleDirectory),
                            onClick = { appState.openCurrentModuleDirectory() },
                            shortcut = KeyAction.NavigateOpenModuleDirectory.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                        Item(
                            string(Strings.MenuNavigateOpenLocationProjectLocation),
                            onClick = { appState.openProjectLocation() },
                            shortcut = KeyAction.NavigateOpenProjectLocation.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                    }
                    Item(
                        string(Strings.MenuNavigateNextEntry),
                        onClick = { appState.nextEntry() },
                        shortcut = KeyAction.NavigateNextEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canGoNextEntryOrSample,
                    )
                    Item(
                        string(Strings.MenuNavigatePreviousEntry),
                        onClick = { appState.previousEntry() },
                        shortcut = KeyAction.NavigatePreviousEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canGoPreviousEntryOrSample,
                    )
                    Item(
                        string(Strings.MenuNavigateNextSample),
                        onClick = { appState.nextSample() },
                        shortcut = KeyAction.NavigateNextSample.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canGoNextEntryOrSample,
                    )
                    Item(
                        string(Strings.MenuNavigatePreviousSample),
                        onClick = { appState.previousSample() },
                        shortcut = KeyAction.NavigatePreviousSample.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.canGoPreviousEntryOrSample,
                    )
                    Item(
                        string(Strings.MenuNavigateJumpToEntry),
                        onClick = { appState.openJumpToEntryDialog() },
                        shortcut = KeyAction.NavigateJumpToEntry.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    if (appState.shouldShowModuleNavigation()) {
                        Item(
                            string(Strings.MenuNavigateNextModule),
                            onClick = { appState.nextModule() },
                            shortcut = KeyAction.NavigateNextModule.getKeyShortCut(),
                            enabled = appState.isEditorActive && appState.canGoNextModule,
                        )
                        Item(
                            string(Strings.MenuNavigatePreviousModule),
                            onClick = { appState.previousModule() },
                            shortcut = KeyAction.NavigatePreviousModule.getKeyShortCut(),
                            enabled = appState.isEditorActive && appState.canGoPreviousModule,
                        )
                        Item(
                            string(Strings.MenuNavigateJumpToModule),
                            onClick = { appState.openJumpToModuleDialog() },
                            shortcut = KeyAction.NavigateJumpToModule.getKeyShortCut(),
                            enabled = appState.isEditorActive,
                        )
                    }
                    Item(
                        string(Strings.MenuNavigateScrollFit),
                        onClick = { appState.scrollFitViewModel.emit() },
                        shortcut = KeyAction.NavigateScrollFit.getKeyShortCut(),
                        enabled = appState.isEditorActive && appState.isScrollFitEnabled,
                    )
                }
            }
            Menu(string(Strings.MenuTools), mnemonic = 'T') {
                if (appState != null) {
                    val appRecord by appState.appRecordFlow.collectAsState()
                    Menu(string(Strings.MenuToolsBatchEdit)) {
                        appState.getActivePlugins(Plugin.Type.Macro)
                            .map { it to it.isMacroExecutable(appState) }
                            .runIf(appRecord.showDisabledMacroPluginItems.not()) {
                                filter { it.second }
                            }
                            .forEach {
                                Item(
                                    it.first.displayedName.get(),
                                    onClick = { appState.openMacroPluginDialog(it.first) },
                                    enabled = it.second,
                                )
                            }
                        Separator()
                        Item(
                            string(Strings.MenuToolsBatchEditQuickLaunchManager),
                            onClick = { appState.openQuickLaunchManagerDialog() },
                            shortcut = KeyAction.ManageMacroPluginsQuickLaunch.getKeyShortCut(),
                        )
                        appRecord.getUsedPluginQuickLaunchSlots().forEach { slot ->
                            val quickLaunch = appRecord.getPluginQuickLaunch(slot)
                            if (quickLaunch != null) {
                                val plugin = appState.getActivePlugins(Plugin.Type.Macro)
                                    .find { it.name == quickLaunch.pluginName }
                                if (plugin != null) {
                                    Item(
                                        string(
                                            Strings.MenuToolsBatchEditQuickLaunch,
                                            slot + 1,
                                            plugin.displayedName.get(),
                                        ),
                                        onClick = { quickLaunch.launch(plugin, appState, slot) },
                                        shortcut = KeyAction.getQuickLaunchAction(slot).getKeyShortCut(),
                                        enabled = plugin.isMacroExecutable(appState),
                                    )
                                }
                            }
                        }
                        Separator()
                        CheckboxItem(
                            string(Strings.MenuToolsBatchEditShowDisabledItems),
                            checked = appRecord.showDisabledMacroPluginItems,
                            onCheckedChange = {
                                appState.appRecordStore.update { copy(showDisabledMacroPluginItems = it) }
                            },
                            shortcut = KeyAction.ToggleShowDisabledMacroPlugins.getKeyShortCut(),
                        )
                        Item(
                            string(Strings.MenuToolsBatchEditManagePlugins),
                            onClick = { appState.openCustomizableItemManagerDialog(CustomizableItem.Type.MacroPlugin) },
                            shortcut = KeyAction.ManageMacroPlugins.getKeyShortCut(),
                        )
                    }
                    Item(
                        string(Strings.MenuToolsPrerender),
                        onClick = { appState.openPrerenderDialog() },
                        shortcut = KeyAction.PrerenderAll.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuToolsSyncSample),
                        onClick = { appState.openEntrySampleSyncDialog() },
                        shortcut = KeyAction.SyncSample.getKeyShortCut(),
                        enabled = appState.isEditorActive,
                    )
                    Item(
                        string(Strings.MenuToolsRecycleMemory),
                        onClick = { System.gc() },
                        shortcut = KeyAction.RecycleMemory.getKeyShortCut(),
                    )
                    Item(
                        string(Strings.MenuToolsFileNameNormalizer),
                        onClick = { appState.openFileNameNormalizerDialog() },
                        shortcut = KeyAction.FileNameNormalizer.getKeyShortCut(),
                    )
                }
            }
            Menu(string(Strings.MenuSettings), mnemonic = 'S') {
                if (appState != null) {
                    Item(
                        string(Strings.MenuSettingsPreferences),
                        onClick = { appState.openPreferencesDialog() },
                        shortcut = KeyAction.Preferences.getKeyShortCut(),
                    )
                    Item(
                        string(Strings.MenuSettingsLabelers),
                        onClick = { appState.openCustomizableItemManagerDialog(CustomizableItem.Type.Labeler) },
                        shortcut = KeyAction.ManageLabelers.getKeyShortCut(),
                    )
                    Item(
                        string(Strings.MenuSettingsTemplatePlugins),
                        onClick = { appState.openCustomizableItemManagerDialog(CustomizableItem.Type.TemplatePlugin) },
                        shortcut = KeyAction.ManageTemplatePlugins.getKeyShortCut(),
                    )
                    Item(
                        string(Strings.MenuSettingsTracking),
                        onClick = { appState.openTrackingSettingsDialog() },
                        shortcut = KeyAction.ManageTracking.getKeyShortCut(),
                    )
                }
            }
            Menu(string(Strings.MenuHelp), mnemonic = 'H') {
                Item(
                    string(Strings.MenuHelpCheckForUpdates),
                    onClick = { appState?.checkUpdates(isAuto = false) },
                    shortcut = KeyAction.CheckForUpdates.getKeyShortCut(),
                )
                Item(
                    string(Strings.MenuHelpOpenLogDirectory),
                    onClick = { Desktop.getDesktop().open(Log.LoggingPath.toFile()) },
                    shortcut = KeyAction.OpenLogDirectory.getKeyShortCut(),
                )
                if (appState != null) {
                    val appRecord by appState.appRecordFlow.collectAsState()
                    CheckboxItem(
                        string(Strings.MenuHelpIncludeInfoLog),
                        checked = appRecord.includeInfoLog,
                        onCheckedChange = { appState.appRecordStore.update { copy(includeInfoLog = it) } },
                    )
                }
                Item(
                    string(Strings.MenuHelpOpenHomePage),
                    onClick = { Url.open(Url.HOME_PAGE) },
                    shortcut = KeyAction.OpenHomePage.getKeyShortCut(),
                )
                Item(
                    string(Strings.MenuHelpOpenLatestRelease),
                    onClick = { Url.open(Url.LATEST_RELEASE) },
                    shortcut = KeyAction.OpenLatestRelease.getKeyShortCut(),
                )
                Item(
                    string(Strings.MenuHelpOpenGitHub),
                    onClick = { Url.open(Url.PROJECT_GIT_HUB) },
                    shortcut = KeyAction.OpenGitHub.getKeyShortCut(),
                )
                Item(
                    string(Strings.MenuHelpJoinDiscord),
                    onClick = { Url.open(Url.DISCORD_INVITATION) },
                    shortcut = KeyAction.JoinDiscord.getKeyShortCut(),
                )
                Item(
                    string(Strings.MenuHelpAbout),
                    onClick = { appState?.openAboutDialog() },
                    shortcut = KeyAction.About.getKeyShortCut(),
                )
            }
            if (isDebug) {
                Menu("Debug") {
                    if (appState != null) {
                        Item(
                            "Throw Exception",
                            onClick = { errorForDebug = IllegalStateException("Test exception from menu") },
                        )
                        Item(
                            "Show Caught Error (Pending exit)",
                            onClick = {
                                appState.showError(
                                    IllegalStateException("Test caught exception from menu"),
                                    pendingAction = AppErrorState.ErrorPendingAction.Exit,
                                )
                            },
                        )
                        Item(
                            "Export AppConfig",
                            onClick = { CustomAppConfFile.writeText(appState.appConf.stringifyJson()) },
                        )
                        Item(
                            "Copy AppConfig",
                            onClick = { Clipboard.copyToClipboard(appState.appConf.stringifyJson()) },
                        )
                        Item(
                            "GC",
                            onClick = { System.gc() },
                        )
                        CheckboxItem(
                            "Show chunk border",
                            checked = DebugState.isShowingChunkBorder,
                            onCheckedChange = { DebugState.isShowingChunkBorder = it },
                        )
                        CheckboxItem(
                            "Print memory usage",
                            checked = DebugState.printMemoryUsage,
                            onCheckedChange = { DebugState.printMemoryUsage = it },
                        )
                        Item(
                            "Open App Directory",
                            onClick = { Desktop.getDesktop().open(AppDir) },
                        )
                        CheckboxItem(
                            "Force Custom File Dialog",
                            checked = DebugState.forceUseCustomFileDialog,
                            onCheckedChange = { DebugState.forceUseCustomFileDialog = it },
                        )
                        Item(
                            "Export current labeler",
                            enabled = appState.hasProject,
                            onClick = {
                                appState.mainScope.launch(Dispatchers.IO) {
                                    val labeler = appState.project?.labelerConf ?: return@launch
                                    labeler.install(AppDir.resolve("debug"))
                                        .onSuccess { Desktop.getDesktop().open(it.parentFile) }
                                        .onFailure { Log.error(it) }
                                }
                            },
                        )
                        Item(
                            "Show Font Preview Dialog",
                            onClick = { DebugState.isShowingFontPreviewDialog = true },
                        )
                    }
                }
            }
        }
    }
}
