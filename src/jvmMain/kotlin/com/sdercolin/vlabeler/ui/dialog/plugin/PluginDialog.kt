package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.FileWithEncoding
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.ReversedRow
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.dialog.SaveFileDialog
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.detectEncoding
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.getDirectory
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toFileOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun rememberPluginDialogState(
    plugin: Plugin,
    appRecordStore: AppRecordStore,
    snackbarHostState: SnackbarHostState,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    project: Project?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    load: (ParamMap) -> Unit,
    executable: Boolean,
    slot: Int?,
) = remember(plugin, paramMap, savedParamMap, submit, save, load) {
    PluginDialogState(
        plugin = plugin,
        appRecordStore = appRecordStore,
        snackbarHostState = snackbarHostState,
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        project = project,
        submit = submit,
        save = save,
        load = load,
        executable = executable,
        slot = slot,
    )
}

@Composable
fun TemplatePluginDialog(
    appConf: AppConf,
    appRecordStore: AppRecordStore,
    snackbarHostState: SnackbarHostState,
    plugin: Plugin,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    load: (ParamMap) -> Unit,
) = PluginDialog(
    appConf = appConf,
    appRecordStore = appRecordStore,
    state = rememberPluginDialogState(
        plugin = plugin,
        appRecordStore = appRecordStore,
        snackbarHostState = snackbarHostState,
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        project = null,
        submit = submit,
        save = save,
        load = load,
        executable = false,
        slot = null,
    ),
)

@Immutable
data class MacroPluginDialogArgs(
    val plugin: Plugin,
    val paramMap: ParamMap,
    val slot: Int? = null,
)

@Composable
fun MacroPluginDialog(
    appConf: AppConf,
    appRecordStore: AppRecordStore,
    snackbarHostState: SnackbarHostState,
    args: MacroPluginDialogArgs,
    project: Project?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    load: (ParamMap) -> Unit,
    executable: Boolean = true,
) = PluginDialog(
    appConf = appConf,
    appRecordStore = appRecordStore,
    state = rememberPluginDialogState(
        plugin = args.plugin,
        appRecordStore = appRecordStore,
        snackbarHostState = snackbarHostState,
        paramMap = args.paramMap,
        savedParamMap = args.paramMap,
        project = project,
        submit = submit,
        save = save,
        load = load,
        executable = executable,
        slot = args.slot,
    ),
)

@Composable
private fun rememberLabelerDialogState(
    labeler: LabelerConf,
    isExistingProject: Boolean,
    snackbarHostState: SnackbarHostState,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    load: (ParamMap) -> Unit,
) = remember(labeler, paramMap, savedParamMap, submit, save, load) {
    LabelerDialogState(
        labeler = labeler,
        isExistingProject = isExistingProject,
        snackbarHostState = snackbarHostState,
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        submit = submit,
        save = save,
        load = load,
    )
}

@Composable
fun LabelerPluginDialog(
    isExistingProject: Boolean,
    appConf: AppConf,
    appRecordStore: AppRecordStore,
    snackbarHostState: SnackbarHostState,
    labeler: LabelerConf,
    paramMap: ParamMap,
    savedParamMap: ParamMap,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    load: (ParamMap) -> Unit,
) = PluginDialog(
    appConf = appConf,
    appRecordStore = appRecordStore,
    state = rememberLabelerDialogState(
        isExistingProject = isExistingProject,
        labeler = labeler,
        snackbarHostState = snackbarHostState,
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        submit = submit,
        save = save,
        load = load,
    ),
)

@Composable
private fun PluginDialog(
    appConf: AppConf,
    appRecordStore: AppRecordStore,
    state: BasePluginDialogState,
) {
    val appRecord = appRecordStore.stateFlow.collectAsState()
    val dialogState = rememberResizableDialogState(appRecord)
    DialogWindow(
        title = string(Strings.PluginDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = { state.cancel() },
        state = dialogState,
    ) {
        LaunchSaveDialogSize(dialogState, appRecordStore)
        AppTheme(appConf.view) {
            Content(state, appRecordStore)
        }
    }
}

@Composable
private fun rememberResizableDialogState(appRecord: State<AppRecord>): DialogState {
    val dialogSize = remember { appRecord.value.pluginDialogSizeDp }
    return rememberDialogState(width = dialogSize.first.dp, height = dialogSize.second.dp)
}

private fun AppRecordStore.saveDialogSize(dpSize: DpSize) {
    val size = dpSize.width.value to dpSize.height.value
    Log.info("Plugin Dialog size changed: $size")
    update { copy(pluginDialogSizeDp = size) }
}

@Composable
private fun LaunchSaveDialogSize(
    dialogState: DialogState,
    appRecordStore: AppRecordStore,
) {
    LaunchedEffect(dialogState) {
        snapshotFlow { dialogState.size }
            .onEach(appRecordStore::saveDialogSize)
            .launchIn(this)
    }
}

@Composable
private fun Content(state: BasePluginDialogState, appRecordStore: AppRecordStore) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val plugin = state.basePlugin
    val needJsClient = state.needJsClient
    val js by produceState(null as JavaScript?, needJsClient) {
        if (needJsClient && value == null) {
            value = withContext(Dispatchers.IO) { JavaScript() }
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f)
                    .padding(horizontal = 45.dp, vertical = 30.dp)
                    .verticalScroll(scrollState),
            ) {
                ReversedRow(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.requiredWidthIn(min = 116.dp)) {
                        ExportButton(state, appRecordStore, coroutineScope)
                        ImportButton(state, appRecordStore, coroutineScope)
                        IconButton(
                            modifier = Modifier.padding(start = 10.dp),
                            onClick = state::reset,
                            enabled = state.canReset(),
                        ) {
                            Icon(Icons.Default.RestartAlt, null)
                        }
                        IconButton(
                            modifier = Modifier.padding(start = 10.dp),
                            onClick = state::save,
                            enabled = state.canSave(),
                        ) {
                            Icon(Icons.Default.Save, null)
                        }
                    }
                    Title(plugin)
                }
                Spacer(Modifier.height(15.dp))
                Info(plugin, contactAuthor = { state.openEmail() })
                Spacer(Modifier.height(5.dp))
                plugin.description.get().takeIf { it.isNotEmpty() }?.let {
                    Description(it)
                }
                if (plugin.website.isNotBlank()) {
                    Website(plugin.website, state::openWebsite)
                }
                Spacer(Modifier.height(25.dp))
                if (state.hasParams) {
                    Params(state, js, coroutineScope)
                    Spacer(Modifier.height(25.dp))
                }
                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { state.cancel() },
                    ) {
                        Text(string(Strings.CommonCancel))
                    }
                    Spacer(Modifier.width(40.dp))

                    ConfirmButton(
                        onClick = state::apply,
                        enabled = state.isAllValid(),
                        text = string(
                            if (plugin.isSelfExecutable && (state as? PluginDialogState)?.executable == true) {
                                Strings.PluginDialogExecute
                            } else {
                                Strings.CommonOkay
                            },
                        ),
                    )
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scrollState), Modifier.width(15.dp))
        }
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(
                state.snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Snackbar(
                    it,
                    actionColor = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
private fun Title(plugin: BasePlugin) {
    Text(
        text = plugin.displayedName.get(),
        style = MaterialTheme.typography.h4,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Info(plugin: BasePlugin, contactAuthor: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = string(Strings.PluginDialogInfoAuthor, plugin.author),
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = string(Strings.PluginDialogInfoVersion, plugin.version),
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (plugin.email.isNotBlank()) {
            SingleClickableText(
                text = string(Strings.PluginDialogInfoContact),
                style = MaterialTheme.typography.caption,
                onClick = contactAuthor,
            )
        }
    }
}

@Composable
private fun Website(website: String, openWebsite: () -> Unit) {
    SingleClickableText(
        modifier = Modifier.padding(vertical = 3.dp),
        text = website,
        style = MaterialTheme.typography.caption,
        onClick = openWebsite,
    )
}

@Composable
private fun Description(description: String) {
    Text(
        modifier = Modifier.padding(vertical = 3.dp),
        text = description,
        style = MaterialTheme.typography.caption,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Params(state: BasePluginDialogState, js: JavaScript?, coroutineScope: CoroutineScope) {
    Column(
        modifier = Modifier.background(color = White20, shape = RoundedCornerShape(10.dp))
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        state.params.indices.map { i ->
            if (state.isChangeable(state.paramDefs[i].name).not()) return@map i to false
            val dependingParamName = state.paramDefs[i].enableIf?.split("=")?.first() ?: return@map i to true
            val dependingParam = state.paramDefs.firstOrNull { it.name == dependingParamName } ?: return@map i to false
            val dependingParamValue = state.params[state.paramDefs.indexOf(dependingParam)]
            val dependingParamTrueValues = state.paramDefs[i].enableIf?.split("=")?.getOrNull(1)?.split("|")
            if (dependingParamTrueValues != null) {
                i to dependingParamTrueValues.contains(dependingParamValue.toString())
            } else {
                i to dependingParam.eval(dependingParamValue)
            }
        }.forEach { (i, enabled) ->
            val labelInRow = state.isParamInRow(i)
            Column(Modifier.heightIn(min = 60.dp)) {
                if (!labelInRow) {
                    ParamLabel(state, i, enabled)
                    Spacer(Modifier.height(10.dp))
                }
                Row {
                    if (labelInRow) {
                        Column(Modifier.width(300.dp).align(Alignment.CenterVertically)) {
                            ParamLabel(state, i, enabled)
                        }
                        Spacer(Modifier.width(20.dp))
                    }
                    val def = state.paramDefs[i]
                    val value = state.params[i]
                    val isError = state.isValid(i).not()
                    val onValueChange = { newValue: Any -> state.update(i, newValue) }
                    if (state.acceptParamType(def.type)) {
                        when (def) {
                            is Parameter.BooleanParam -> ParamSwitch(value as Boolean, onValueChange, enabled)
                            is Parameter.EnumParam -> ParamDropDown(
                                options = def.options,
                                optionDisplayedNames = def.optionDisplayedNames ?: def.options.map { it.toLocalized() },
                                value = value as String,
                                onValueChange = onValueChange,
                                enabled = enabled,
                            )
                            is Parameter.FloatParam -> ParamNumberTextField(
                                value = value as Float,
                                onValueChange = onValueChange,
                                isError = isError,
                                parse = { it.toFloatOrNull() },
                                onParseErrorChange = { state.setParseError(i, it) },
                                enabled = enabled,
                            )
                            is Parameter.IntParam -> ParamNumberTextField(
                                value = value as Int,
                                onValueChange = onValueChange,
                                isError = isError,
                                parse = { it.toIntOrNull() },
                                onParseErrorChange = { state.setParseError(i, it) },
                                enabled = enabled,
                            )
                            is Parameter.StringParam -> ParamTextField(
                                value = value as String,
                                onValueChange = onValueChange,
                                isError = isError,
                                isLong = true,
                                singleLine = def.multiLine.not(),
                                enabled = enabled,
                            )
                            is Parameter.EntrySelectorParam -> ParamEntrySelector(
                                labelerConf = state.project?.labelerConf,
                                value = value as EntrySelector,
                                onValueChange = onValueChange,
                                isError = isError,
                                onParseErrorChange = { state.setParseError(i, it) },
                                entries = state.project?.currentModule?.entries,
                                js = js,
                                enabled = enabled,
                                onError = {
                                    coroutineScope.launch {
                                        state.showSnackbar(
                                            it.message ?: it.toString(),
                                        )
                                    }
                                },
                            )
                            is Parameter.FileParam -> ParamFileTextField(
                                value = value as FileWithEncoding,
                                onValueChange = onValueChange,
                                param = def,
                                isError = isError,
                                enabled = enabled,
                            )
                            is Parameter.RawFileParam -> ParamRawFileTextField(
                                value = value as String,
                                onValueChange = onValueChange,
                                param = def,
                                isError = isError,
                                enabled = enabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamLabel(state: BasePluginDialogState, index: Int, enabled: Boolean) {
    val alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        Text(
            text = state.getLabel(index),
            style = MaterialTheme.typography.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val description = state.getDescription(index)
        if (description.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RowScope.ParamSwitch(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Switch(
        modifier = Modifier.align(Alignment.CenterVertically),
        checked = value,
        onCheckedChange = onValueChange,
        colors = getSwitchColors(),
        enabled = enabled,
    )
}

@Composable
private fun ParamDropDown(
    options: List<String>,
    optionDisplayedNames: List<LocalizedJsonString>,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val text = optionDisplayedNames[options.indexOf(value)].get()
    Box {
        TextField(
            modifier = Modifier.widthIn(min = 200.dp),
            value = text,
            onValueChange = {},
            readOnly = true,
            maxLines = 1,
            leadingIcon = {
                IconButton(onClick = { expanded = true }, enabled = enabled) {
                    Icon(Icons.Default.ExpandMore, null)
                }
            },
            enabled = enabled,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.zip(optionDisplayedNames).forEach {
                DropdownMenuItem(
                    onClick = {
                        onValueChange(it.first)
                        expanded = false
                    },
                ) {
                    Text(text = it.second.get())
                }
            }
        }
    }
}

@Composable
private fun <T : Number> ParamNumberTextField(
    value: T,
    onValueChange: (T) -> Unit,
    isError: Boolean,
    parse: (String) -> T?,
    onParseErrorChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    var stringValue by remember { mutableStateOf(value.toString()) }
    var isParsingFailed by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (parse(stringValue) == value) return@LaunchedEffect
        isParsingFailed = false
        onParseErrorChange(false)
        stringValue = value.toString()
    }

    fun onNewStringValue(newValue: String) {
        stringValue = newValue
        val parsed = parse(newValue)
        if (parsed == null) {
            isParsingFailed = true
        } else {
            isParsingFailed = false
            onValueChange(parsed)
        }
        onParseErrorChange(isParsingFailed)
    }

    ParamTextField(
        value = stringValue,
        onValueChange = ::onNewStringValue,
        isError = isError || isParsingFailed,
        isLong = false,
        singleLine = true,
        enabled = enabled,
    )
}

@Composable
private fun ParamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isLong: Boolean,
    singleLine: Boolean,
    enabled: Boolean,
) {
    val modifier = if (!isLong) Modifier.widthIn(min = 200.dp) else Modifier.fillMaxWidth()
    TextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        isError = isError,
        enabled = enabled,
    )
}

@Composable
private fun ParamFileTextField(
    value: FileWithEncoding,
    onValueChange: (FileWithEncoding) -> Unit,
    param: Parameter.FileParam,
    isError: Boolean,
    enabled: Boolean,
) {
    var isShowingFilePicker by remember { mutableStateOf(false) }
    var encodingExpanded by remember { mutableStateOf(false) }
    var encoding by remember(value) { mutableStateOf(value.encoding.orEmpty()) }
    var path by remember(value) { mutableStateOf(value.file.orEmpty()) }

    fun submit() {
        onValueChange(FileWithEncoding(path, encoding.takeIf { it.isNotBlank() }))
    }

    LaunchedEffect(value) {
        if (value.encoding == null) {
            value.file?.toFileOrNull(ensureExists = true, ensureIsFile = true)?.let { file ->
                file.readBytes().detectEncoding()?.let { detected ->
                    encoding = AvailableEncodings.find { encodingNameEquals(detected, it) } ?: detected
                    submit()
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        TextField(
            modifier = Modifier.weight(1f),
            value = path,
            onValueChange = {
                path = it
                submit()
            },
            singleLine = true,
            isError = isError,
            enabled = enabled,
            trailingIcon = {
                IconButton(onClick = { isShowingFilePicker = true }, enabled = enabled) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            },
        )
        Box {
            TextField(
                modifier = Modifier.width(200.dp),
                value = encoding,
                onValueChange = {},
                label = { Text(string(Strings.StarterNewEncoding)) },
                readOnly = true,
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = { encodingExpanded = true }, enabled = enabled) {
                        Icon(Icons.Default.ExpandMore, null)
                    }
                },
                enabled = enabled,
            )
            DropdownMenu(
                expanded = encodingExpanded,
                onDismissRequest = { encodingExpanded = false },
            ) {
                AvailableEncodings.forEach {
                    DropdownMenuItem(
                        onClick = {
                            encoding = it
                            encodingExpanded = false
                            submit()
                        },
                    ) {
                        Text(text = it)
                    }
                }
            }
        }
    }
    if (isShowingFilePicker) {
        val file = value.file?.toFile()
        OpenFileDialog(
            title = param.label.get(),
            initialDirectory = file?.parent,
            initialFileName = file?.name,
            extensions = param.acceptExtensions,
        ) { parent, name ->
            isShowingFilePicker = false
            if (parent == null || name == null) return@OpenFileDialog
            path = File(parent, name).absolutePath
            submit()
        }
    }
}

@Composable
private fun ParamRawFileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    param: Parameter.RawFileParam,
    isError: Boolean,
    enabled: Boolean,
) {
    var isShowingFilePicker by remember { mutableStateOf(false) }
    var path by remember(value) { mutableStateOf(value) }

    fun submit() {
        onValueChange(path)
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        TextField(
            modifier = Modifier.weight(1f),
            value = path,
            onValueChange = {
                path = it
                submit()
            },
            singleLine = true,
            isError = isError,
            enabled = enabled,
            trailingIcon = {
                IconButton(onClick = { isShowingFilePicker = true }, enabled = enabled) {
                    Icon(Icons.Default.FolderOpen, null)
                }
            },
        )
    }
    if (isShowingFilePicker) {
        val file = path.toFileOrNull(ensureExists = true, ensureIsFile = true)
        val directoryMode = param.isFolder
        OpenFileDialog(
            title = param.label.get(),
            initialDirectory = file?.parent,
            initialFileName = file?.name,
            extensions = param.acceptExtensions,
            directoryMode = directoryMode,
        ) { parent, name ->
            isShowingFilePicker = false
            if (parent == null || name == null) return@OpenFileDialog
            val newFile = File(parent, name)
            path = if (directoryMode) {
                newFile.getDirectory().absolutePath
            } else {
                newFile.absolutePath
            }
            submit()
        }
    }
}

@Composable
private fun ImportButton(state: BasePluginDialogState, appRecordStore: AppRecordStore, coroutineScope: CoroutineScope) {
    var expanded by remember { mutableStateOf(false) }
    var isShowingFilePicker by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 10.dp)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FileDownload, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val record = appRecordStore.stateFlow.collectAsState()
            state.getImportablePresets(record.value)
                .forEach { item ->
                    DropdownMenuItem(
                        enabled = item.available,
                        onClick = {
                            expanded = false
                            when (item) {
                                is BasePluginPresetItem.Memory -> {
                                    coroutineScope.launch { state.import(item.resolve()) }
                                }
                                is BasePluginPresetItem.File -> {
                                    isShowingFilePicker = true
                                }
                            }
                        },
                    ) {
                        Text(item.getImportText())
                    }
                }
        }
    }
    if (isShowingFilePicker) {
        OpenFileDialog(
            title = string(Strings.PluginDialogImportFromFile),
            extensions = listOf("json"),
        ) { parent, name ->
            isShowingFilePicker = false
            if (parent == null || name == null) return@OpenFileDialog
            coroutineScope.launch(Dispatchers.IO) {
                state.import(BasePluginPresetItem.File.resolve(File(parent, name)))
            }
        }
    }
}

@Composable
private fun ExportButton(state: BasePluginDialogState, appRecordStore: AppRecordStore, coroutineScope: CoroutineScope) {
    var expanded by remember { mutableStateOf(false) }
    var isShowingFilePicker by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 10.dp)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FileUpload, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val record = appRecordStore.stateFlow.collectAsState()
            state.getExportablePresets(record.value)
                .forEach { item ->
                    DropdownMenuItem(
                        enabled = item.available,
                        onClick = {
                            expanded = false
                            when (item) {
                                is BasePluginPresetItem.Memory -> {
                                    coroutineScope.launch { state.export(item.resolve()) }
                                }
                                is BasePluginPresetItem.File -> {
                                    isShowingFilePicker = true
                                }
                            }
                        },
                    ) {
                        Text(item.getExportText())
                    }
                }
        }
    }
    if (isShowingFilePicker) {
        SaveFileDialog(
            title = string(Strings.PluginDialogExportToFile),
            initialFileName = state.basePlugin.getSavedParamsFile().name,
            extensions = listOf("json"),
        ) { parent, name ->
            isShowingFilePicker = false
            if (parent == null || name == null) return@SaveFileDialog
            coroutineScope.launch(Dispatchers.IO) {
                state.export(BasePluginPresetItem.File.resolve(File(parent, name)))
            }
        }
    }
}
