@file:OptIn(ExperimentalFoundationApi::class)

package com.sdercolin.vlabeler.ui.starter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.common.Tooltip
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.dialog.WarningDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.LabelerPluginDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.TemplatePluginDialog
import com.sdercolin.vlabeler.ui.starter.ProjectCreatorState.ContentType
import com.sdercolin.vlabeler.ui.starter.ProjectCreatorState.Page
import com.sdercolin.vlabeler.ui.string.LocalLanguage
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import kotlinx.coroutines.CoroutineScope

@Composable
fun ProjectCreator(
    appState: AppState,
    cancel: () -> Unit,
    activeLabelerConfs: List<LabelerConf>,
    activeTemplatePlugins: List<Plugin>,
    appRecordStore: AppRecordStore,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    state: ProjectCreatorState = rememberProjectCreatorState(
        appState,
        coroutineScope,
        activeLabelerConfs,
        activeTemplatePlugins,
        appRecordStore,
    ),
) {
    Surface(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Title(page = state.page.text)
            Content(state)
            Divider(Modifier.height(1.dp), color = Black50)
            ButtonBar(
                hasPrevious = state.hasPrevious,
                hasNext = state.hasNext,
                isError = state.hasError,
                goPrevious = state::goPrevious,
                goNext = state::goNext,
                cancel = cancel,
            )
        }
        if (state.isLoading) {
            CircularProgress()
        }
        state.currentPathPicker?.let { picker ->
            PickerDialog(state, picker)
        }
        state.warningText?.let {
            WarningDialog(string(it), finish = { state.warningText = null }, style = WarningTextStyle.Warning)
        }
    }
}

@Composable
private fun Title(page: Strings) {
    Text(
        modifier = Modifier.padding(top = 70.dp, bottom = 45.dp, start = 60.dp, end = 60.dp),
        text = "${string(Strings.StarterNewProjectTitle)} - ${string(page)}",
        style = MaterialTheme.typography.h4,
        maxLines = 1,
    )
}

@Composable
private fun ColumnScope.Content(state: ProjectCreatorState) {
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 60.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BasicItems(state)
            val hasAdvancedSettings = when (state.page) {
                Page.Directory -> true
                Page.Labeler -> false
                Page.DataSource -> true
            }
            if (hasAdvancedSettings) {
                AdvancedSwitch(state)
                if (state.isDetailExpanded) {
                    AdvancedItems(state)
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scrollState), Modifier.width(15.dp))
    }
}

@Composable
private fun BasicItems(state: ProjectCreatorState) {
    when (state.page) {
        Page.Directory -> DirectoryPageBasic(state)
        Page.Labeler -> LabelerPageBasic(state)
        Page.DataSource -> DataSourcePageBasic(state)
    }
}

@Composable
private fun DirectoryPageBasic(state: ProjectCreatorState) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = state.sampleDirectory,
        onValueChange = state::updateSampleDirectory,
        label = { Text(string(Strings.StarterNewSampleDirectory)) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { state.pickSampleDirectory() }) {
                Icon(Icons.Default.FolderOpen, null)
            }
        },
        isError = state.isSampleDirectoryValid().not(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            modifier = Modifier.widthIn(min = 400.dp),
            value = state.projectName,
            onValueChange = state::updateProjectName,
            label = { Text(string(Strings.StarterNewProjectName)) },
            singleLine = true,
            isError = state.isProjectNameValid().not(),
        )
        if (state.isProjectFileExisting()) {
            Spacer(Modifier.width(15.dp))
            TooltipArea(
                tooltip = { Tooltip(string(Strings.StarterNewProjectNameWarning)) },
            ) {
                Icon(Icons.Default.Warning, null, tint = DarkYellow, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun LabelerPageBasic(state: ProjectCreatorState) {
    val scrollState = rememberScrollState()
    Column {
        Text(
            style = MaterialTheme.typography.h6,
            text = string(Strings.StarterNewLabelerCategory),
        )
        Row(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            state.labelerCategories.forEach { category ->
                TextButton(
                    modifier = Modifier.widthIn(min = 120.dp).heightIn(min = 80.dp),
                    onClick = { state.updateLabelerCategory(category) },
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = if (state.labelerCategory == category) {
                            MaterialTheme.colors.primaryVariant
                        } else {
                            White.copy(alpha = 0.1f)
                        },
                        contentColor = MaterialTheme.colors.onSurface,
                    ),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        text = category.ifEmpty { string(Strings.CommonOthers) },
                    )
                }
            }
        }
        HorizontalScrollbar(rememberScrollbarAdapter(scrollState), Modifier.height(10.dp))
    }

    Text(
        style = MaterialTheme.typography.h6,
        text = string(Strings.StarterNewLabeler),
    )
    var labelerDialogShown by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        LabelerSelector(state)
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = { labelerDialogShown = true }) {
            val color = if (state.labelerError) {
                MaterialTheme.colors.error
            } else {
                LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            }
            Icon(Icons.Default.Settings, null, tint = color)
        }
    }

    LabelerSummary(
        labeler = state.labeler,
        openWebsite = { state.openWebsite(it) },
    )

    if (labelerDialogShown) {
        val snackbarHostState = remember { SnackbarHostState() }
        LabelerPluginDialog(
            isExistingProject = false,
            appConf = state.appConf,
            appRecordStore = state.appRecordStore,
            snackbarHostState = snackbarHostState,
            labeler = requireNotNull(state.labeler),
            paramMap = requireNotNull(state.labelerParams),
            savedParamMap = requireNotNull(state.labelerSavedParams),
            submit = {
                if (it != null) state.updateLabelerParams(it)
                labelerDialogShown = false
            },
            save = { state.saveLabelerParams(it) },
            load = { state.updateLabelerParams(it) },
        )
    }
}

@Composable
private fun LabelerSelector(state: ProjectCreatorState) {
    LaunchedEffect(Unit) {
        state.updateLabeler(state.labeler)
    }
    Box {
        var expanded by remember { mutableStateOf(false) }
        TextField(
            modifier = Modifier.widthIn(min = 400.dp),
            value = state.labeler.displayedName.get(),
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            leadingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ExpandMore, null)
                }
            },
        )
        DropdownMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.selectableLabelers.forEach { conf ->
                DropdownMenuItem(
                    onClick = {
                        state.updateLabeler(conf)
                        expanded = false
                    },
                ) {
                    Text(text = conf.displayedName.get())
                }
            }
        }
    }
}

@Composable
private fun LabelerSummary(labeler: LabelerConf, openWebsite: (LabelerConf) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = labeler.displayedName.get(),
            style = MaterialTheme.typography.h5,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = string(Strings.PluginDialogInfoAuthor, labeler.author),
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = string(Strings.PluginDialogInfoVersion, labeler.version),
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(5.dp))
        labeler.description.get().takeIf { it.isNotEmpty() }?.let {
            Text(
                modifier = Modifier.padding(vertical = 3.dp),
                text = it,
                style = MaterialTheme.typography.body2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (labeler.website.isNotBlank()) {
            SingleClickableText(
                modifier = Modifier.padding(vertical = 3.dp),
                text = labeler.website,
                style = MaterialTheme.typography.caption,
                onClick = { openWebsite(labeler) },
            )
        }
    }
}

@Composable
private fun DataSourcePageBasic(state: ProjectCreatorState) {
    Text(
        style = MaterialTheme.typography.h6,
        text = string(Strings.StarterNewContentType),
    )
    val language = LocalLanguage.current
    LaunchedEffect(Unit) {
        state.selectContentType(state.contentType, language)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        state.selectableContentTypes.forEach { type ->
            TextButton(
                modifier = Modifier.widthIn(min = 120.dp).heightIn(min = 80.dp),
                onClick = { state.selectContentType(type, language) },
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (state.contentType == type) {
                        MaterialTheme.colors.primaryVariant
                    } else {
                        White.copy(alpha = 0.1f)
                    },
                    contentColor = MaterialTheme.colors.onSurface,
                ),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    text = string(type.text),
                )
            }
        }
    }
    when (state.contentType) {
        ContentType.Default -> Unit
        ContentType.File -> {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.inputFile,
                onValueChange = { state.updateInputFile(it, editedByUser = true) },
                label = { Text(string(Strings.StarterNewInputFile, state.labeler.extension)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { state.pickInputFile() }) {
                        Icon(Icons.Default.FolderOpen, null)
                    }
                },
                isError = state.isInputFileValid().not(),
            )
        }
        ContentType.Plugin -> {
            Spacer(Modifier.height(10.dp))
            Text(
                style = MaterialTheme.typography.h6,
                text = string(Strings.StarterNewTemplatePlugin),
            )
            var pluginDialogShown by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TemplatePluginSelector(state)
                Spacer(Modifier.width(10.dp))
                IconButton(
                    enabled = state.templatePlugin != null,
                    onClick = { pluginDialogShown = true },
                ) {
                    val color = if (state.templatePluginError) {
                        MaterialTheme.colors.error
                    } else {
                        LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                    }
                    Icon(Icons.Default.Settings, null, tint = color)
                }
            }
            if (pluginDialogShown) {
                val snackbarHostState = remember { SnackbarHostState() }
                TemplatePluginDialog(
                    appConf = state.appConf,
                    appRecordStore = state.appRecordStore,
                    snackbarHostState = snackbarHostState,
                    plugin = requireNotNull(state.templatePlugin),
                    paramMap = requireNotNull(state.templatePluginParams),
                    savedParamMap = requireNotNull(state.templatePluginSavedParams),
                    submit = {
                        if (it != null) state.updatePluginParams(it)
                        pluginDialogShown = false
                    },
                    load = { state.updatePluginParams(it) },
                    save = { state.savePluginParams(it) },
                )
            }
        }
    }
}

@Composable
private fun TemplatePluginSelector(state: ProjectCreatorState) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        TextField(
            modifier = Modifier.widthIn(min = 400.dp),
            value = state.getTemplatePluginName(),
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            leadingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ExpandMore, null)
                }
            },
        )
        val language = LocalLanguage.current
        DropdownMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.getSupportedPlugins(language).forEach { plugin ->
                DropdownMenuItem(
                    onClick = {
                        state.updatePlugin(plugin)
                        expanded = false
                    },
                ) {
                    Text(text = plugin.displayedName.get())
                }
            }
        }
    }
}

@Composable
private fun AdvancedSwitch(state: ProjectCreatorState) {
    Row(
        modifier = Modifier.padding(top = 25.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FreeSizedIconButton(onClick = { state.toggleDetailExpanded() }) {
            val icon = if (state.isDetailExpanded) {
                Icons.Default.ExpandLess
            } else {
                Icons.Default.ExpandMore
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
        val isBasicSettingsError = when (state.page) {
            Page.Directory ->
                state.isSampleDirectoryValid().not() || state.isProjectNameValid().not()
            Page.Labeler -> false
            Page.DataSource -> false
        }
        val isAdvancedSettingsError = when (state.page) {
            Page.Directory ->
                state.isWorkingDirectoryValid().not() || state.isCacheDirectoryValid().not()
            Page.Labeler -> false
            Page.DataSource -> false
        }
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = string(Strings.StarterNewAdvancedSettings),
            style = MaterialTheme.typography.body2,
            color = if (isBasicSettingsError.not() && isAdvancedSettingsError) {
                MaterialTheme.colors.error
            } else {
                MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            },
        )
    }
}

@Composable
private fun AdvancedItems(state: ProjectCreatorState) {
    when (state.page) {
        Page.Directory -> DirectoryPageAdvanced(state)
        Page.Labeler -> Unit
        Page.DataSource -> DataSourcePageAdvanced(state)
    }
}

@Composable
private fun DirectoryPageAdvanced(state: ProjectCreatorState) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = state.workingDirectory,
        onValueChange = state::updateWorkingDirectory,
        label = { Text(string(Strings.StarterNewWorkingDirectory)) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { state.pickWorkingDirectory() }) {
                Icon(Icons.Default.FolderOpen, null)
            }
        },
        isError = state.isWorkingDirectoryValid().not(),
    )
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = state.cacheDirectory,
        onValueChange = state::updateCacheDirectory,
        label = { Text(string(Strings.StarterNewCacheDirectory)) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { state.pickCacheDirectory() }) {
                Icon(Icons.Default.FolderOpen, null)
            }
        },
        isError = state.isCacheDirectoryValid().not(),
    )
}

@Composable
private fun DataSourcePageAdvanced(state: ProjectCreatorState) {
    EncodingSelector(state)
    AutoExportSwitch(state)
}

@Composable
private fun EncodingSelector(state: ProjectCreatorState) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextField(
            modifier = Modifier.widthIn(min = 200.dp),
            value = state.encoding,
            onValueChange = { },
            readOnly = true,
            label = { Text(string(Strings.StarterNewEncoding)) },
            singleLine = true,
            leadingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ExpandMore, null)
                }
            },
        )
        DropdownMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.encodings.forEach { encodingName ->
                DropdownMenuItem(
                    onClick = {
                        state.encoding = encodingName
                        expanded = false
                    },
                ) {
                    Text(text = encodingName)
                }
            }
        }
    }
}

@Composable
private fun AutoExportSwitch(state: ProjectCreatorState) {
    Column {
        val contentAlpha = if (state.canAutoExport) {
            LocalContentAlpha.current
        } else {
            ContentAlpha.disabled
        }
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.autoExport,
                    onCheckedChange = { state.toggleAutoExport(it) },
                    colors = getSwitchColors(),
                    enabled = state.canAutoExport,
                )
                Spacer(Modifier.width(10.dp))
                Text(string(Strings.StarterNewAutoExport))
            }
        }
        Text(
            modifier = Modifier.padding(top = 5.dp, start = 10.dp).widthIn(max = 600.dp),
            text = string(Strings.StarterNewAutoExportHelp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ButtonBar(
    hasPrevious: Boolean,
    hasNext: Boolean,
    isError: Boolean,
    goPrevious: () -> Unit,
    goNext: () -> Unit,
    cancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { cancel() }) {
            Text(string(Strings.CommonCancel))
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            enabled = hasPrevious,
            onClick = goPrevious,
        ) {
            Text(string(Strings.CommonPrevious))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(
            enabled = !isError,
            onClick = goNext,
            text = if (hasNext) string(Strings.CommonNext) else string(Strings.CommonFinish),
        )
    }
}

@Composable
private fun PickerDialog(
    state: ProjectCreatorState,
    picker: PathPicker,
) {
    val title = state.getFilePickerTitle(picker)
    val initial = state.getFilePickerInitialDirectory(picker)
    val extensions = state.getFilePickerExtensions(picker)
    val directoryMode = state.getFilePickerDirectoryMode(picker)
    OpenFileDialog(
        title = title,
        initialDirectory = initial,
        extensions = extensions,
        directoryMode = directoryMode,
    ) { parent, name ->
        state.handleFilePickerResult(picker, parent, name)
    }
}
