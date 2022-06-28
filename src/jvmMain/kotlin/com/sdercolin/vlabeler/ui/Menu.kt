package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.io.openProject
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.Menu(
    mainScope: CoroutineScope,
    appState: AppState
) {
    val showSnackbar: (String) -> Unit = { mainScope.launch { appState.snackbarHostState.showSnackbar(it) } }

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
            Menu(string(Strings.MenuFileOpenRecent)) {
                appState.appRecord.recentProjectPathsWithDisplayNames.forEach { (path, displayName) ->
                    Item(
                        text = displayName,
                        onClick = { openProject(mainScope, File(path), appState) }
                    )
                }
                Separator()
                Item(
                    text = string(Strings.MenuFileOpenRecentClear),
                    enabled = appState.appRecord.recentProjects.isNotEmpty(),
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
                        showSnackbar = showSnackbar
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
                        showSnackbar = showSnackbar
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
                enabled = appState.isEditorActive
            )
        }
        if (isDebug) {
            Menu("Debug") {
                Item(
                    "Throw exception",
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
