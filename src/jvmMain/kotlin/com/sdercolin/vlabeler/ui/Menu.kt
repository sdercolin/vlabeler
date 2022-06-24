package com.sdercolin.vlabeler.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.ui.labeler.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.Menu(
    appState: MutableState<AppState>,
    scrollFitViewModel: ScrollFitViewModel,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = { coroutineScope.launch { snackbarHostState.showSnackbar(it) } }

    MenuBar {
        Menu(string(Strings.MenuFile), mnemonic = 'F') {
            Item(
                string(Strings.MenuFileNewProject),
                onClick = { appState.update { configureNewProject() } },
                shortcut = getKeyShortCut(Key.N, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileOpen),
                onClick = { appState.update { requestOpenProject() } },
                shortcut = getKeyShortCut(Key.O, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileSave),
                onClick = { appState.update { requestSave() } },
                enabled = appState.value.hasUnsavedChanges,
                shortcut = getKeyShortCut(Key.S, ctrl = true)
            )
            Item(
                string(Strings.MenuFileSaveAs),
                onClick = { appState.update { openSaveAsProjectDialog() } },
                shortcut = getKeyShortCut(Key.S, ctrl = true, shift = true),
                enabled = appState.value.hasProject
            )
            Item(
                string(Strings.MenuFileExport),
                onClick = { appState.update { requestExport() } },
                enabled = appState.value.hasProject
            )
            Item(
                string(Strings.MenuFileClose),
                onClick = { appState.update { requestCloseProject() } },
                enabled = appState.value.hasProject
            )
        }
        Menu(string(Strings.MenuEdit), mnemonic = 'E') {
            Item(
                string(Strings.MenuEditUndo),
                shortcut = getKeyShortCut(Key.Z, ctrl = true),
                onClick = { appState.update { undo() } },
                enabled = appState.value.history.canUndo
            )
            Item(
                string(Strings.MenuEditRedo),
                shortcut = getKeyShortCut(Key.Z, ctrl = true, shift = true),
                onClick = { appState.update { redo() } },
                enabled = appState.value.history.canRedo
            )
            Item(
                string(Strings.MenuEditRenameEntry),
                onClick = {
                    appState.update {
                        openEditEntryNameDialog(
                            duplicate = false,
                            showSnackbar = showSnackbar
                        )
                    }
                },
                enabled = appState.value.isEditorActive
            )
            Item(
                string(Strings.MenuEditDuplicateEntry),
                onClick = {
                    appState.update {
                        openEditEntryNameDialog(
                            duplicate = true,
                            showSnackbar = showSnackbar
                        )
                    }
                },
                enabled = appState.value.isEditorActive
            )
            Item(
                string(Strings.MenuEditRemoveEntry),
                onClick = { appState.update { confirmIfRemoveCurrentEntry() } },
                enabled = appState.value.isEditorActive && appState.value.canRemoveCurrentEntry
            )
            Item(
                string(Strings.MenuEditJumpToEntry),
                shortcut = getKeyShortCut(Key.G, ctrl = true),
                onClick = { appState.update { openJumpToEntryDialog() } },
                enabled = appState.value.isEditorActive
            )
            Item(
                string(Strings.MenuEditScrollFit),
                shortcut = getKeyShortCut(Key.F),
                onClick = { scrollFitViewModel.emit() },
                enabled = appState.value.isEditorActive
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
