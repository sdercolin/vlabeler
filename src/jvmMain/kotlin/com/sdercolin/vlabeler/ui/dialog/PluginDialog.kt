package com.sdercolin.vlabeler.ui.dialog

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.common.ClickableText
import com.sdercolin.vlabeler.ui.common.ReversedRow
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toParamMap
import com.sdercolin.vlabeler.util.toUri
import java.awt.Desktop

class PluginDialogState(
    val plugin: Plugin,
    private val paramMap: ParamMap,
    private val submit: (ParamMap?) -> Unit,
    private val save: (ParamMap) -> Unit
) {
    val paramDefs = plugin.parameters?.list.orEmpty()
    val params = mutableStateListOf(*paramMap.map { it.value.toString() }.toTypedArray())
    val hasParams get() = paramDefs.isNotEmpty()

    private fun getCurrentParamMap() = params.mapIndexed { index, textValue ->
        val def = paramDefs[index]
        def.name to parseTextValue(textValue, def)
    }.toMap().toParamMap()

    fun apply() {
        submit(getCurrentParamMap())
    }

    private fun parseTextValue(textValue: String, def: Plugin.Parameter<*>): Any {
        return when (def) {
            is Plugin.Parameter.BooleanParam -> textValue.toBooleanStrict()
            is Plugin.Parameter.EnumParam -> textValue
            is Plugin.Parameter.FloatParam -> textValue.toFloat()
            is Plugin.Parameter.IntParam -> textValue.toInt()
            is Plugin.Parameter.StringParam -> textValue
        }
    }

    fun cancel() {
        submit(null)
    }

    fun getLabel(index: Int): String {
        return paramDefs[index].label
    }

    fun getDescription(index: Int): String {
        val description = paramDefs[index].description
        val range: Pair<String?, String?> = when (val def = paramDefs[index]) {
            is Plugin.Parameter.FloatParam -> def.min?.toString() to def.max?.toString()
            is Plugin.Parameter.IntParam -> def.min?.toString() to def.max?.toString()
            else -> null to null
        }
        val suffix = when {
            range.first != null && range.second != null ->
                string(Strings.PluginDialogDescriptionMinMax, range.first, range.second)
            range.first != null ->
                string(Strings.PluginDialogDescriptionMin, range.first)
            range.second != null ->
                string(Strings.PluginDialogDescriptionMax, range.second)
            else -> ""
        }
        return description?.plus("\n").orEmpty() + suffix
    }

    fun isValid(index: Int): Boolean {
        val value = params[index]
        return when (val def = paramDefs[index]) {
            is Plugin.Parameter.BooleanParam -> true
            is Plugin.Parameter.EnumParam -> true
            is Plugin.Parameter.FloatParam -> {
                val floatValue = value.toFloatOrNull()
                if (floatValue != null) {
                    floatValue in (def.min ?: Float.NEGATIVE_INFINITY)..(def.max ?: Float.POSITIVE_INFINITY)
                } else false
            }
            is Plugin.Parameter.IntParam -> {
                val intValue = value.toIntOrNull()
                if (intValue != null) {
                    intValue in (def.min ?: Int.MIN_VALUE)..(def.max ?: Int.MAX_VALUE)
                } else false
            }
            is Plugin.Parameter.StringParam -> {
                val fulfillMultiLine = if (def.multiLine.not()) {
                    value.lines().size < 2
                } else true
                val fulfillOptional = if (def.optional.not()) {
                    value.isNotEmpty()
                } else true
                fulfillMultiLine && fulfillOptional
            }
        }
    }

    fun isAllValid() = params.indices.all { isValid(it) }

    fun update(index: Int, value: String) {
        params[index] = value
    }

    fun canReset() = paramDefs.indices.all {
        params[it] == paramDefs[it].defaultValue.toString()
    }

    fun reset() {
        paramDefs.indices.forEach {
            params[it] = paramDefs[it].defaultValue.toString()
        }
    }

    fun openEmail() {
        Desktop.getDesktop().browse("mailto:${plugin.email}".toUri())
    }

    fun openWebsite() {
        val uri = plugin.website.takeIf { it.isNotBlank() }?.toUri() ?: return
        Desktop.getDesktop().browse(uri)
    }

    fun canSave(): Boolean {
        val current = runCatching { getCurrentParamMap().toList() }.getOrNull() ?: return false
        val saved = paramMap.toList()
        val changed = saved.indices.all { saved[it] == current[it] }.not()
        return changed && isAllValid()
    }

    fun save() {
        save(getCurrentParamMap())
    }
}

@Composable
private fun rememberState(
    plugin: Plugin,
    paramMap: ParamMap,
    submit: (ParamMap?) -> Unit,
    save: (ParamMap) -> Unit
) = remember(paramMap) { PluginDialogState(plugin, paramMap, submit, save) }

@Composable
fun PluginDialog(
    plugin: Plugin,
    paramMap: ParamMap,
    submit: (ParamMap?) -> Unit,
    save: (Plugin, ParamMap) -> Unit,
    state: PluginDialogState = rememberState(plugin, paramMap, submit, save = { save(plugin, it) })
) {
    Dialog(
        title = string(Strings.PluginDialogTitle),
        icon = painterResource("icon.ico"),
        onCloseRequest = { state.cancel() },
        state = DialogState(width = 800.dp, height = 500.dp)
    ) {
        AppTheme {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: PluginDialogState) {
    val scrollState = rememberScrollState()
    val plugin = state.plugin
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
                    Params(state)
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
                        Text(string(Strings.CommonOkay))
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
private fun Params(state: PluginDialogState) {
    Column(
        modifier = Modifier.background(color = White20, shape = RoundedCornerShape(10.dp))
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        state.params.indices.forEach { i ->
            Row(Modifier.heightIn(min = 60.dp)) {
                Column(Modifier.width(300.dp).align(Alignment.CenterVertically)) {
                    Text(
                        text = state.getLabel(i),
                        style = MaterialTheme.typography.body2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val description = state.getDescription(i)
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
                Spacer(Modifier.width(20.dp))
                val def = state.paramDefs[i]
                val value = state.params[i]
                val isError = state.isValid(i).not()
                val onValueChange = { newValue: String -> state.update(i, newValue) }
                when (def) {
                    is Plugin.Parameter.BooleanParam -> ParamSwitch(value, onValueChange)
                    is Plugin.Parameter.EnumParam -> ParamDropDown(def.options, value, onValueChange)
                    is Plugin.Parameter.FloatParam ->
                        ParamTextField(value, onValueChange, isError, isLong = false, singleLine = true)
                    is Plugin.Parameter.IntParam ->
                        ParamTextField(value, onValueChange, isError, isLong = false, singleLine = true)
                    is Plugin.Parameter.StringParam ->
                        ParamTextField(value, onValueChange, isError, isLong = true, singleLine = def.multiLine.not())
                }
            }
        }
    }
}

@Composable
private fun RowScope.ParamSwitch(
    value: String,
    onValueChange: (String) -> Unit
) {
    Switch(
        modifier = Modifier.align(Alignment.CenterVertically),
        checked = value.toBooleanStrict(),
        onCheckedChange = { onValueChange(it.toString()) },
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
