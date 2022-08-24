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
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.common.ClickableText
import com.sdercolin.vlabeler.ui.common.ReversedRow
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

@Composable
private fun rememberState(
    plugin: Plugin,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    project: Project?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit
) = remember(plugin, paramMap, savedParamMap, submit, save) {
    PluginDialogState(
        plugin,
        paramMap,
        savedParamMap,
        project,
        submit,
        save
    )
}

@Composable
fun TemplatePluginDialog(
    appRecordStore: AppRecordStore,
    plugin: Plugin,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit
) = PluginDialog(
    appRecordStore = appRecordStore,
    plugin = plugin,
    paramMap = paramMap,
    savedParamMap = savedParamMap,
    project = null,
    submit = submit,
    save = save
)

@Composable
fun MacroPluginDialog(
    appRecordStore: AppRecordStore,
    plugin: Plugin,
    paramMap: ParamMap,
    project: Project?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit
) = PluginDialog(
    appRecordStore = appRecordStore,
    plugin = plugin,
    paramMap = paramMap,
    savedParamMap = paramMap,
    project = project,
    submit = submit,
    save = save
)

@Composable
private fun PluginDialog(
    appRecordStore: AppRecordStore,
    plugin: Plugin,
    paramMap: ParamMap,
    savedParamMap: ParamMap?,
    project: Project?,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit,
    state: PluginDialogState = rememberState(
        plugin,
        paramMap,
        savedParamMap,
        project,
        submit,
        save
    )
) {
    val appRecord = appRecordStore.stateFlow.collectAsState()
    val dialogState = rememberResizableDialogState(appRecord)
    Dialog(
        title = string(Strings.PluginDialogTitle),
        icon = painterResource("icon.ico"),
        onCloseRequest = { state.cancel() },
        state = dialogState
    ) {
        LaunchSaveDialogSize(dialogState, appRecordStore)
        AppTheme {
            Content(state)
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
    appRecordStore: AppRecordStore
) {
    LaunchedEffect(dialogState) {
        snapshotFlow { dialogState.size }
            .onEach(appRecordStore::saveDialogSize)
            .launchIn(this)
    }
}

@Composable
private fun Content(state: PluginDialogState) {
    val scrollState = rememberScrollState()
    val plugin = state.plugin
    val needJsClient = plugin.parameters?.list?.any { it.type == Plugin.ParameterType.EntrySelector } == true
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
                    .verticalScroll(scrollState)
            ) {
                ReversedRow(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.requiredWidthIn(min = 116.dp)) {
                        IconButton(
                            modifier = Modifier.padding(start = 10.dp),
                            onClick = state::reset,
                            enabled = !state.canReset()
                        ) {
                            Icon(Icons.Default.RestartAlt, null)
                        }
                        IconButton(
                            modifier = Modifier.padding(start = 10.dp),
                            onClick = state::save,
                            enabled = state.canSave()
                        ) {
                            Icon(Icons.Default.Save, null)
                        }
                    }
                    Title(plugin)
                }
                Spacer(Modifier.height(15.dp))
                Info(plugin, contactAuthor = { state.openEmail() })
                Spacer(Modifier.height(5.dp))
                if (plugin.description.isNotBlank()) {
                    Description(plugin.description)
                }
                if (plugin.website.isNotBlank()) {
                    Website(plugin.website, state::openWebsite)
                }
                Spacer(Modifier.height(25.dp))
                if (state.hasParams) {
                    Params(state, js)
                    Spacer(Modifier.height(25.dp))
                }
                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { state.cancel() }
                    ) {
                        Text(string(Strings.CommonCancel))
                    }
                    Spacer(Modifier.width(40.dp))
                    Button(
                        enabled = state.isAllValid(),
                        onClick = { state.apply() }
                    ) {
                        val strings = when (plugin.type) {
                            Plugin.Type.Template -> Strings.CommonOkay
                            Plugin.Type.Macro -> Strings.PluginDialogExecute
                        }
                        Text(string(strings))
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scrollState), Modifier.width(15.dp))
        }
    }
}

@Composable
private fun Title(plugin: Plugin) {
    Text(
        text = plugin.displayedName,
        style = MaterialTheme.typography.h4,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun Info(plugin: Plugin, contactAuthor: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = string(Strings.PluginDialogInfoAuthor, plugin.author),
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = string(Strings.PluginDialogInfoVersion, plugin.version),
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (plugin.email.isNotBlank()) {
            ClickableText(
                text = string(Strings.PluginDialogInfoContact),
                style = MaterialTheme.typography.caption,
                onClick = contactAuthor
            )
        }
    }
}

@Composable
private fun Website(website: String, openWebsite: () -> Unit) {
    ClickableText(
        modifier = Modifier.padding(vertical = 3.dp),
        text = website,
        style = MaterialTheme.typography.caption,
        onClick = openWebsite
    )
}

@Composable
private fun Description(description: String) {
    Text(
        modifier = Modifier.padding(vertical = 3.dp),
        text = description,
        style = MaterialTheme.typography.caption,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun Params(state: PluginDialogState, js: JavaScript?) {
    Column(
        modifier = Modifier.background(color = White20, shape = RoundedCornerShape(10.dp))
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        state.params.indices.forEach { i ->
            val labelInRow = state.isParamInRow(i)
            Column(Modifier.heightIn(min = 60.dp)) {
                if (!labelInRow) {
                    ParamLabel(state, i)
                    Spacer(Modifier.height(10.dp))
                }
                Row {
                    if (labelInRow) {
                        Column(Modifier.width(300.dp).align(Alignment.CenterVertically)) {
                            ParamLabel(state, i)
                        }
                        Spacer(Modifier.width(20.dp))
                    }
                    val def = state.paramDefs[i]
                    val value = state.params[i]
                    val isError = state.isValid(i).not()
                    val onValueChange = { newValue: Any -> state.update(i, newValue) }
                    when (def) {
                        is Plugin.Parameter.BooleanParam -> ParamSwitch(value as Boolean, onValueChange)
                        is Plugin.Parameter.EnumParam -> ParamDropDown(def.options, value as String, onValueChange)
                        is Plugin.Parameter.FloatParam ->
                            ParamNumberTextField(
                                value as Float,
                                onValueChange,
                                isError,
                                parse = { it.toFloatOrNull() },
                                onParseErrorChange = { state.setParseError(i, it) }
                            )
                        is Plugin.Parameter.IntParam ->
                            ParamNumberTextField(
                                value as Int,
                                onValueChange,
                                isError,
                                parse = { it.toIntOrNull() },
                                onParseErrorChange = { state.setParseError(i, it) }
                            )
                        is Plugin.Parameter.StringParam ->
                            ParamTextField(
                                value as String,
                                onValueChange,
                                isError,
                                isLong = true,
                                singleLine = def.multiLine.not()
                            )
                        is Plugin.Parameter.EntrySelectorParam ->
                            ParamEntrySelector(
                                requireNotNull(state.project).labelerConf,
                                value as EntrySelector,
                                onValueChange,
                                isError,
                                onParseErrorChange = { state.setParseError(i, it) },
                                requireNotNull(state.project).entries,
                                js
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamLabel(state: PluginDialogState, index: Int) {
    Text(
        text = state.getLabel(index),
        style = MaterialTheme.typography.body2,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    val description = state.getDescription(index)
    if (description.isNotBlank()) {
        Spacer(Modifier.height(5.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.caption,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.ParamSwitch(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Switch(
        modifier = Modifier.align(Alignment.CenterVertically),
        checked = value,
        onCheckedChange = onValueChange,
        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
    )
}

@Composable
private fun ParamDropDown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextField(
            modifier = Modifier.widthIn(min = 200.dp),
            value = value,
            onValueChange = {},
            readOnly = true,
            maxLines = 1,
            leadingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    onClick = {
                        onValueChange(it)
                        expanded = false
                    }
                ) {
                    Text(text = it)
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
    onParseErrorChange: (Boolean) -> Unit
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
        stringValue,
        ::onNewStringValue,
        isError || isParsingFailed,
        isLong = false,
        singleLine = true
    )
}

@Composable
private fun ParamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isLong: Boolean,
    singleLine: Boolean
) {
    val modifier = if (!isLong) Modifier.widthIn(min = 200.dp) else Modifier.fillMaxWidth()
    TextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        isError = isError
    )
}
