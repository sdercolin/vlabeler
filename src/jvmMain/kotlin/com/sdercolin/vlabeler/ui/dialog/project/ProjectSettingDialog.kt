package com.sdercolin.vlabeler.ui.dialog.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.dialog.plugin.LabelerPluginDialog
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.AvailableEncodings

@Composable
fun rememberProjectSettingDialogState(appState: AppState, finish: () -> Unit) =
    remember(appState) {
        ProjectSettingDialogState(appState, finish)
    }

@Composable
fun ProjectListDialog(
    appState: AppState,
    finish: () -> Unit,
    state: ProjectSettingDialogState = rememberProjectSettingDialogState(appState, finish),
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50),
        contentAlignment = Alignment.Center,
    ) {
        Surface {
            Box(Modifier.fillMaxSize(0.8f).plainClickable()) {
                Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 45.dp)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = string(Strings.ProjectSettingDialogTitle),
                        style = MaterialTheme.typography.h4,
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Content(state)
                    Spacer(modifier = Modifier.height(25.dp))
                    ButtonBar(state)
                }
            }
        }
    }
    if (state.isShowingLabelerDialog) {
        val snackbarHostState = remember { SnackbarHostState() }
        LabelerPluginDialog(
            isExistingProject = true,
            appConf = state.appState.appConf,
            appRecordStore = state.appState.appRecordStore,
            snackbarHostState = snackbarHostState,
            labeler = requireNotNull(state.project.labelerConf),
            paramMap = requireNotNull(state.labelerParams),
            savedParamMap = requireNotNull(state.labelerSavedParams),
            submit = {
                if (it != null) state.updateLabelerParams(it)
                state.isShowingLabelerDialog = false
            },
            save = { state.saveLabelerParams(it) },
            load = { state.updateLabelerParams(it) },
        )
    }
}

@Composable
private fun ColumnScope.Content(state: ProjectSettingDialogState) {
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            ItemRow(title = Strings.StarterNewEncoding) {
                SelectionBox(
                    value = state.encoding,
                    onSelect = { state.encoding = it },
                    options = AvailableEncodings,
                    getText = { it },
                )
            }
            ItemRow(title = Strings.StarterNewAutoExport) {
                Switch(
                    enabled = state.canChangeAutoExport,
                    checked = state.autoExport,
                    onCheckedChange = { state.autoExport = it },
                    colors = getSwitchColors(),
                )
            }
            ItemRow(title = Strings.StarterNewLabeler) {
                SelectionBox(
                    value = state.project.labelerConf,
                    onSelect = { },
                    options = listOf(state.project.labelerConf),
                    getText = { it.displayedName.get() },
                    enabled = false,
                )
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = { state.isShowingLabelerDialog = true }) {
                    val color = if (state.labelerError) {
                        MaterialTheme.colors.error
                    } else {
                        LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                    }
                    Icon(Icons.Default.Settings, null, tint = color)
                }
            }
        }
    }
}

@Composable
private fun ItemRow(title: Strings, item: @Composable () -> Unit) {
    Row(Modifier.padding(vertical = 10.dp)) {
        Text(
            modifier = Modifier.padding(top = 14.dp).widthIn(min = 200.dp, max = 400.dp),
            text = string(title),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(25.dp))
        item()
    }
}

@Composable
private fun ButtonBar(state: ProjectSettingDialogState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = state::cancel) {
            Text(text = string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(onClick = state::submit, enabled = state.labelerError.not())
    }
}
