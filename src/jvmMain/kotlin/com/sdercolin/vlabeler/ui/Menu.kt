package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.sdercolin.vlabeler.env.isMacOS
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.update

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.Menu(
    projectState: MutableState<Project?>,
    appState: MutableState<AppState>
) {
    MenuBar {
        Menu(string(Strings.MenuFile), mnemonic = 'F') {
            Item(
                string(Strings.MenuFileNewProject),
                onClick = { appState.update { copy(isConfiguringNewProject = true) } },
                shortcut = getKeyShortCut(Key.N, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileOpen),
                onClick = { appState.update { copy(isShowingOpenProjectDialog = true) } },
                shortcut = getKeyShortCut(Key.O, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileSave),
                onClick = { appState.update { requestSave() } },
                enabled = appState.value.isSaveEnabled,
                shortcut = getKeyShortCut(Key.S, ctrl = true)
            )
            Item(
                string(Strings.MenuFileSaveAs),
                onClick = { appState.update { copy(isShowingSaveAsProjectDialog = true) } },
                shortcut = getKeyShortCut(Key.S, ctrl = true, shift = true)
            )
            Item(
                string(Strings.MenuFileClose),
                onClick = { projectState.value = null },
                enabled = projectState.value != null
            )
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
