package com.sdercolin.vlabeler.ui.dialog.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.HeaderFooterColumn
import com.sdercolin.vlabeler.ui.common.MediumDialogContainer
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.dialog.SaveFileDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.LabelerPluginDialog
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.getDirectory
import com.sdercolin.vlabeler.util.toFile
import java.io.File

@Composable
fun rememberProjectSettingDialogState(appState: AppState, finish: () -> Unit) =
    remember(appState) {
        ProjectSettingDialogState(appState, finish)
    }

@Composable
fun ProjectSettingDialog(
    appState: AppState,
    finish: () -> Unit,
    state: ProjectSettingDialogState = rememberProjectSettingDialogState(appState, finish),
) {
    MediumDialogContainer(wrapHeight = true) {
        HeaderFooterColumn(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 45.dp),
            header = {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = string(Strings.ProjectSettingDialogTitle),
                    style = MaterialTheme.typography.h5,
                )
                Spacer(modifier = Modifier.height(25.dp))
            },
            footer = {
                Spacer(modifier = Modifier.height(25.dp))
                ButtonBar(state)
            },
        ) {
            Content(state)
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

    if (state.isShowingRootDirectoryDialog) {
        val current = state.rootDirectory.toFile().takeIf { state.isRootDirectoryValid }
        OpenFileDialog(
            title = string(Strings.ChooseSampleDirectoryDialogTitle),
            initialDirectory = current?.absolutePath,
            directoryMode = true,
        ) { parent, name ->
            state.isShowingRootDirectoryDialog = false
            if (parent == null || name == null) return@OpenFileDialog
            state.updateRootDirectory(File(parent, name).getDirectory().absolutePath)
        }
    }

    if (state.isShowingCacheDirectoryDialog) {
        val current = state.cacheDirectory.toFile().takeIf { state.isCacheDirectoryValid }
        OpenFileDialog(
            title = string(Strings.ChooseCacheDirectoryDialogTitle),
            initialDirectory = current?.absolutePath,
            directoryMode = true,
        ) { parent, name ->
            state.isShowingCacheDirectoryDialog = false
            if (parent == null || name == null) return@OpenFileDialog
            state.updateCacheDirectory(File(parent, name).getDirectory().absolutePath)
        }
    }

    if (state.isShowingOutputFileDialog) {
        val current = state.outputFile?.toFile()?.takeIf { state.isOutputFileValid }
        SaveFileDialog(
            title = string(Strings.ProjectSettingOutputFileSelectorDialogTitle),
            initialDirectory = current?.parentFile?.absolutePath
                ?: state.project.currentModule.getSampleDirectory(state.project).absolutePath,
            initialFileName = current?.name?.ifEmpty { null }
                ?: (state.project.projectName + "." + state.project.labelerConf.extension),
            extensions = listOf(state.project.labelerConf.extension),
        ) { parent, name ->
            state.isShowingOutputFileDialog = false
            if (parent == null || name == null) return@SaveFileDialog
            state.updateOutputFile(File(parent, name).absolutePath)
        }
    }
}

@Composable
private fun ColumnScope.Content(state: ProjectSettingDialogState) {
    ItemRow(
        title = Strings.StarterNewSampleDirectory,
        helperText = null,
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = state.rootDirectory,
            onValueChange = { state.updateRootDirectory(it) },
            singleLine = true,
            isError = state.isRootDirectoryValid.not(),
            trailingIcon = {
                IconButton(
                    onClick = { state.isShowingRootDirectoryDialog = true },
                ) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            },
        )
    }
    ItemRow(
        title = Strings.StarterNewCacheDirectory,
        helperText = null,
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = state.cacheDirectory,
            onValueChange = { state.updateCacheDirectory(it) },
            singleLine = true,
            isError = state.isCacheDirectoryValid.not(),
            trailingIcon = {
                IconButton(
                    onClick = { state.isShowingCacheDirectoryDialog = true },
                ) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            },
        )
    }
    ItemRow(
        title = Strings.ProjectSettingOutputFileLabel,
        helperText = Strings.ProjectSettingOutputFileHelperText,
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = state.outputFile ?: string(Strings.ProjectSettingOutputFileDisabledPlaceholder),
            onValueChange = { state.updateOutputFile(it) },
            singleLine = true,
            isError = state.isOutputFileValid.not(),
            enabled = state.isOutputFileEditable,
            trailingIcon = {
                IconButton(
                    onClick = { state.isShowingOutputFileDialog = true },
                    enabled = state.isOutputFileEditable,
                ) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            },
        )
    }
    ItemRow(title = Strings.StarterNewEncoding, helperText = null) {
        SelectionBox(
            value = state.encoding,
            onSelect = { state.encoding = it },
            options = AvailableEncodings,
            getText = { it },
        )
    }
    ItemRow(title = Strings.StarterNewAutoExport, helperText = Strings.ProjectSettingAutoExportHelperText) {
        Switch(
            enabled = state.canChangeAutoExport,
            checked = state.autoExport,
            onCheckedChange = { state.autoExport = it },
            colors = getSwitchColors(),
        )
    }
    ItemRow(title = Strings.StarterNewLabeler, helperText = null) {
        SelectionBox(
            value = state.project.labelerConf,
            onSelect = { },
            options = listOf(state.project.labelerConf),
            getText = { it.displayedName.get() },
            enabled = false,
            showIcon = false,
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

@Composable
private fun ItemRow(title: Strings, helperText: Strings?, item: @Composable () -> Unit) {
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.widthIn(min = 200.dp, max = 400.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = string(title),
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (helperText != null) {
                Spacer(Modifier.width(10.dp))
                WithTooltip(string(helperText)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
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
        ConfirmButton(onClick = state::submit, enabled = state.isError.not())
    }
}
