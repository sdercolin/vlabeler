package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import kotlinx.coroutines.CoroutineScope
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.Menu(
    mainScope: CoroutineScope,
    appState: AppState
) {
    MenuBar {
        Menu(string(Strings.MenuFile), mnemonic = 'F') {
            Item(
                string(Strings.MenuFileNewProject),
                onClick = { appState.requestOpenProjectCreator() },
                shortcut = getKeyShortCut(Key.N, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileOpen),
                onClick = { appState.requestOpenProject() },
                shortcut = getKeyShortCut(Key.O, ctrl = true, shift = true)
            )
            val appRecord by appState.appRecordFlow.collectAsState()
            Menu(string(Strings.MenuFileOpenRecent)) {
                appRecord.recentProjectPathsWithDisplayNames.forEach { (path, displayName) ->
                    Item(
                        text = displayName,
                        onClick = { appState.requestOpenRecentProject(mainScope, File(path), appState) }
                    )
                }
                Separator()
                Item(
                    text = string(Strings.MenuFileOpenRecentClear),
                    enabled = appRecord.recentProjects.isNotEmpty(),
                    onClick = { appState.clearRecentProjects() }
                )
            }
            Item(
                string(Strings.MenuFileSave),
                onClick = { appState.requestSave() },
                enabled = appState.hasUnsavedChanges,
                shortcut = getKeyShortCut(Key.S, ctrl = true)
            )
            Item(
                string(Strings.MenuFileSaveAs),
                onClick = { appState.openSaveAsProjectDialog() },
                shortcut = getKeyShortCut(Key.S, ctrl = true, shift = true),
                enabled = appState.hasProject
            )
            Item(
                string(Strings.MenuFileExport),
                onClick = { appState.requestExport() },
                enabled = appState.hasProject
            )
            Item(
                string(Strings.MenuFileClose),
                onClick = { appState.requestCloseProject() },
                enabled = appState.hasProject
            )
        }
        Menu(string(Strings.MenuEdit), mnemonic = 'E') {
            Item(
                string(Strings.MenuEditUndo),
                shortcut = getKeyShortCut(Key.Z, ctrl = true),
                onClick = { appState.undo() },
                enabled = appState.history.canUndo
            )
            Item(
                string(Strings.MenuEditRedo),
                shortcut = getKeyShortCut(Key.Z, ctrl = true, shift = true),
                onClick = { appState.redo() },
                enabled = appState.history.canRedo
            )
            Item(
                string(Strings.MenuEditRenameEntry),
                shortcut = getKeyShortCut(Key.R, ctrl = true),
                onClick = {
                    appState.openEditEntryNameDialog(
                        duplicate = false,
                        scope = mainScope
                    )
                },
                enabled = appState.isEditorActive
            )
            Item(
                string(Strings.MenuEditDuplicateEntry),
                shortcut = getKeyShortCut(Key.D, ctrl = true),
                onClick = {
                    appState.openEditEntryNameDialog(
                        duplicate = true,
                        scope = mainScope
                    )
                },
                enabled = appState.isEditorActive
            )
            Item(
                string(Strings.MenuEditRemoveEntry),
                onClick = { appState.confirmIfRemoveCurrentEntry() },
                enabled = appState.isEditorActive && appState.canRemoveCurrentEntry
            )
        }
        Menu(string(Strings.MenuView), mnemonic = 'V') {
            CheckboxItem(
                string(Strings.MenuViewToggleMarker),
                shortcut = getKeyShortCut(Key.Zero, ctrl = true),
                checked = appState.isMarkerDisplayed,
                enabled = appState.isEditorActive,
                onCheckedChange = { appState.isMarkerDisplayed = it }
            )
            CheckboxItem(
                string(Strings.MenuViewToggleProperties),
                shortcut = getKeyShortCut(Key.One, ctrl = true),
                checked = appState.isPropertyViewDisplayed,
                enabled = appState.isEditorActive,
                onCheckedChange = { appState.isPropertyViewDisplayed = it }
            )
            CheckboxItem(
                string(Strings.MenuViewPinEntryList),
                shortcut = getKeyShortCut(Key.Two, ctrl = true),
                checked = appState.isEntryListPinned,
                enabled = appState.isEditorActive,
                onCheckedChange = { appState.isEntryListPinned = it }
            )
        }
        Menu(string(Strings.MenuNavigate), mnemonic = 'N') {
            Item(
                string(Strings.MenuNavigateNextEntry),
                shortcut = getKeyShortCut(Key.DirectionDown),
                onClick = { appState.nextEntry() },
                enabled = appState.isEditorActive && appState.canGoNextEntryOrSample
            )
            Item(
                string(Strings.MenuNavigatePreviousEntry),
                shortcut = getKeyShortCut(Key.DirectionUp),
                onClick = { appState.previousEntry() },
                enabled = appState.isEditorActive && appState.canGoPreviousEntryOrSample
            )
            Item(
                string(Strings.MenuNavigateNextSample),
                shortcut = getKeyShortCut(Key.DirectionDown, ctrl = true),
                onClick = { appState.nextSample() },
                enabled = appState.isEditorActive && appState.canGoNextEntryOrSample
            )
            Item(
                string(Strings.MenuNavigatePreviousSample),
                shortcut = getKeyShortCut(Key.DirectionUp, ctrl = true),
                onClick = { appState.previousSample() },
                enabled = appState.isEditorActive && appState.canGoPreviousEntryOrSample
            )
            Item(
                string(Strings.MenuNavigateJumpToEntry),
                shortcut = getKeyShortCut(Key.G, ctrl = true),
                onClick = { appState.openJumpToEntryDialog() },
                enabled = appState.isEditorActive
            )
            Item(
                string(Strings.MenuNavigateScrollFit),
                shortcut = getKeyShortCut(Key.F),
                onClick = { appState.scrollFitViewModel.emit() },
                enabled = appState.isEditorActive && appState.isScrollFitEnabled
            )
        }
        if (isDebug) {
            Menu("Debug") {
                Item(
                    "Throw Exception",
                    onClick = { throw IllegalStateException("Test exception from menu") }
                )
            }
        }
    }
}

private fun getKeyShortCut(
    key: Key,
    ctrl: Boolean = false,
    shift: Boolean = false,
    alt: Boolean = false
) = KeyShortcut(
    key = key,
    ctrl = if (isMacOS) false else ctrl,
    meta = if (isMacOS) ctrl else false,
    alt = alt,
    shift = shift
)
