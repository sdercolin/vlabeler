@file:OptIn(ExperimentalFoundationApi::class)

package com.sdercolin.vlabeler.ui.starter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Composable
fun ProjectCreator(
    create: (Project) -> Unit,
    cancel: () -> Unit,
    availableLabelerConfs: List<LabelerConf>,
    availableTemplatePlugins: List<Plugin>,
    snackbarHostState: SnackbarHostState,
    appRecordStore: AppRecordStore,
    state: ProjectCreatorState = rememberProjectCreatorState(
        availableLabelerConfs,
        appRecordStore
    ),
) {
    val coroutineScope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 60.dp, vertical = 40.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = string(Strings.StarterNewProject), style = MaterialTheme.typography.h4, maxLines = 1)
                Spacer(Modifier.height(25.dp))
                listOf<@Composable () -> Unit>({
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.sampleDirectory,
                        onValueChange = state::updateSampleDirectory,
                        label = { Text(string(Strings.StarterNewSampleDirectory)) },
                        maxLines = 2,
                        trailingIcon = {
                            IconButton(onClick = { state.pickSampleDirectory() }) {
                                Icon(Icons.Default.FolderOpen, null)
                            }
                        },
                        isError = state.isSampleDirectoryValid().not()
                    )
                }, {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.workingDirectory,
                        onValueChange = state::updateWorkingDirectory,
                        label = { Text(string(Strings.StarterNewWorkingDirectory)) },
                        maxLines = 2,
                        trailingIcon = {
                            IconButton(onClick = { state.pickWorkingDirectory() }) {
                                Icon(Icons.Default.FolderOpen, null)
                            }
                        },
                        isError = state.isWorkingDirectoryValid().not()
                    )
                }, {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            modifier = Modifier.widthIn(min = 400.dp),
                            value = state.projectName,
                            onValueChange = state::updateProjectName,
                            label = { Text(string(Strings.StarterNewProjectName)) },
                            maxLines = 2,
                            isError = state.isProjectNameValid().not()
                        )
                        if (state.isProjectFileExisting()) {
                            Spacer(Modifier.width(20.dp))
                            TooltipArea(
                                tooltip = {
                                    Box(
                                        Modifier.background(
                                            color = MaterialTheme.colors.background,
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                            .padding(10.dp)
                                            .shadow(elevation = 5.dp, shape = RoundedCornerShape(5.dp))
                                    ) {
                                        Text(
                                            string(Strings.StarterNewProjectNameWarning),
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colors.primary)
                            }
                        }
                    }
                }, {
                    Row {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            TextField(
                                modifier = Modifier.widthIn(min = 400.dp),
                                value = state.labeler.displayedName,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(string(Strings.StarterNewLabeler)) },
                                maxLines = 1,
                                leadingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ExpandMore, null)
                                    }
                                }
                            )
                            DropdownMenu(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableLabelerConfs.forEach { conf ->
                                    DropdownMenuItem(
                                        onClick = {
                                            state.updateLabeler(conf)
                                            expanded = false
                                        }
                                    ) {
                                        Text(text = conf.displayedName)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(60.dp))
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            TextField(
                                modifier = Modifier.widthIn(min = 400.dp),
                                value = state.templateName,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(string(Strings.StarterNewTemplatePlugin)) },
                                maxLines = 1,
                                leadingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ExpandMore, null)
                                    }
                                }
                            )
                            DropdownMenu(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        state.updatePlugin(null)
                                        expanded = false
                                    }
                                ) {
                                    Text(text = string(Strings.StarterNewTemplatePluginNone))
                                }
                                state.getSupportedPlugins(availableTemplatePlugins).forEach { plugin ->
                                    DropdownMenuItem(
                                        onClick = {
                                            state.updatePlugin(plugin)
                                            expanded = false
                                        }
                                    ) {
                                        Text(text = plugin.displayedName)
                                    }
                                }
                            }
                        }
                    }
                }, {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.inputFile,
                        onValueChange = { state.updateInputFile(coroutineScope, it) },
                        label = { Text(state.getInputFileLabelText()) },
                        placeholder = state.getInputFilePlaceholderText()?.let { { Text(it) } },
                        enabled = state.isInputFileEnabled(),
                        maxLines = 2,
                        trailingIcon = {
                            IconButton(onClick = { state.pickInputFile() }, enabled = state.isInputFileEnabled()) {
                                Icon(Icons.Default.FolderOpen, null)
                            }
                        },
                        isError = state.isInputFileValid().not()
                    )
                }, {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextField(
                            modifier = Modifier.widthIn(min = 200.dp),
                            value = state.encoding,
                            onValueChange = { },
                            enabled = state.isEncodingSelectionEnabled,
                            readOnly = true,
                            label = { Text(string(Strings.StarterNewEncoding)) },
                            maxLines = 1,
                            leadingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ExpandMore, null)
                                }
                            }
                        )
                        DropdownMenu(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            state.encodings.forEach { encodingName ->
                                DropdownMenuItem(
                                    onClick = {
                                        state.encoding = encodingName
                                        expanded = false
                                    }
                                ) {
                                    Text(text = encodingName)
                                }
                            }
                        }
                    }
                }).forEach {
                    it.invoke()
                    Spacer(Modifier.height(20.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = cancel) {
                        Text(string(Strings.CommonCancel))
                    }
                    Button(
                        onClick = { state.create(coroutineScope, snackbarHostState, create) },
                        enabled = state.isValid()
                    ) {
                        Text(string(Strings.CommonOkay))
                    }
                }
                state.currentPathPicker?.let { picker ->
                    val title = state.getFilePickerTitle(picker)
                    val initial = state.getFilePickerInitialDirectory(picker)
                    val extensions = state.getFilePickerExtensions(picker)
                    val directoryMode = state.getFilePickerDirectoryMode(picker)
                    OpenFileDialog(
                        title = title,
                        initialDirectory = initial,
                        extensions = extensions,
                        directoryMode = directoryMode
                    ) { parent, name ->
                        state.handleFilePickerResult(picker, parent, name, coroutineScope)
                    }
                }
            }
            if (state.isLoading) {
                CircularProgress()
            }
        }
    }
}
