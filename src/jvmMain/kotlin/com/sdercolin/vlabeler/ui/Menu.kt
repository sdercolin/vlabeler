package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.debug.DebugState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.CustomAppConfFile
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.getNullableOrElse
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toUri
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.io.File

@Composable
fun FrameWindowScope.Menu(
    mainScope: CoroutineScope,
    appState: AppState?,
) {
    val keymap = appState?.appConf?.keymaps?.keyActionMap ?: mapOf()

    fun KeyAction.getKeyShortCut() = keymap.getNullableOrElse(this) { this.defaultKeySet }?.toShortCut()

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
                            onClick = { appState.requestOpenRecentProject(mainScope, File(path)) },
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
                    string(Strings.MenuFileExport),
                    onClick = { appState.requestExport() },
                    shortcut = KeyAction.ExportProject.getKeyShortCut(),
                    enabled = appState.hasProject,
                )
                Item(
                    string(Strings.MenuFileInvalidateCaches),
                    onClick = { appState.requestClearCaches(mainScope) },
                    shortcut = KeyAction.InvalidateCaches.getKeyShortCut(),
                    enabled = appState.hasProject,
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
                            index = appState.requireProject().currentIndex,
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
                            index = appState.requireProject().currentIndex,
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
                CheckboxItem(
                    string(Strings.MenuEditMultipleEditMode),
                    checked = appState.project?.multipleEditMode == true,
                    onCheckedChange = { appState.toggleMultipleEditMode(it) },
                    shortcut = KeyAction.ToggleMultipleEditMode.getKeyShortCut(),
                    enabled = appState.isEditorActive && appState.project?.labelerConf?.continuous == true,
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
                Item(
                    string(Strings.MenuViewOpenSampleList),
                    onClick = { appState.openSampleListDialog() },
                    shortcut = KeyAction.OpenSampleList.getKeyShortCut(),
                    enabled = appState.isEditorActive,
                )
            }
        }
        Menu(string(Strings.MenuNavigate), mnemonic = 'N') {
            if (appState != null) {
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
                Menu(string(Strings.MenuToolsBatchEdit)) {
                    appState.getActivePlugins(Plugin.Type.Macro).forEach {
                        Item(
                            it.displayedName,
                            onClick = { appState.openMacroPluginDialog(it) },
                            enabled = it.isMacroExecutable(appState),
                        )
                    }
                    Separator()
                    Item(
                        string(Strings.MenuToolsBatchEditManagePlugins),
                        onClick = { appState.openCustomizableItemManagerDialog(CustomizableItem.Type.MacroPlugin) },
                        shortcut = KeyAction.ManageMacroPlugins.getKeyShortCut(),
                    )
                }
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
            }
        }
        Menu(string(Strings.MenuHelp), mnemonic = 'H') {
            Item(
                string(Strings.MenuHelpOpenLogDirectory),
                onClick = { Desktop.getDesktop().open(Log.LoggingPath.toFile()) },
                shortcut = KeyAction.OpenLogDirectory.getKeyShortCut(),
            )
            Item(
                string(Strings.MenuHelpOpenLatestRelease),
                onClick = { Desktop.getDesktop().browse(Url.LatestRelease.toUri()) },
                shortcut = KeyAction.OpenLatestRelease.getKeyShortCut(),
            )
            Item(
                string(Strings.MenuHelpOpenGitHub),
                onClick = { Desktop.getDesktop().browse(Url.ProjectGitHub.toUri()) },
                shortcut = KeyAction.OpenGitHub.getKeyShortCut(),
            )
            Item(
                string(Strings.MenuHelpJoinDiscord),
                onClick = { Desktop.getDesktop().browse(Url.DiscordInvitation.toUri()) },
                shortcut = KeyAction.JoinDiscord.getKeyShortCut(),
            )
        }
        if (isDebug) {
            Menu("Debug") {
                if (appState != null) {
                    Item(
                        "Throw Exception",
                        onClick = { throw IllegalStateException("Test exception from menu") },
                    )
                    Item(
                        "Export AppConfig",
                        onClick = { CustomAppConfFile.writeText(appState.appConf.stringifyJson()) },
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
                }
            }
        }
    }
}
