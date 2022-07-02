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
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.ParamMap

class PluginDialogState(
    val plugin: Plugin,
    paramMap: ParamMap,
    private val submit: (ParamMap?) -> Unit
) {
    val paramDefs = plugin.parameters?.list.orEmpty()
    val params = mutableStateListOf(*paramMap.map { it.value.toString() }.toTypedArray())
    val hasParams get() = paramDefs.isNotEmpty()

    fun apply() {
        val newMap = params.mapIndexed { index, textValue ->
            val def = paramDefs[index]
            val value = when (def) {
                is Plugin.Parameter.BooleanParam -> textValue.toBooleanStrict()
                is Plugin.Parameter.EnumParam -> textValue
                is Plugin.Parameter.FloatParam -> textValue.toFloat()
                is Plugin.Parameter.IntParam -> textValue.toInt()
                is Plugin.Parameter.StringParam -> textValue
            }
            def.name to value
        }
        submit(newMap.toMap())
    }

    fun cancel() {
        submit(null)
    }

    fun getLabel(index: Int): String {
        val label = paramDefs[index].label
        val range: Pair<String?, String?> = when (val def = paramDefs[index]) {
            is Plugin.Parameter.FloatParam -> def.min?.toString() to def.max?.toString()
            is Plugin.Parameter.IntParam -> def.min?.toString() to def.max?.toString()
            else -> null to null
        }
        val suffix = when {
            range.first != null && range.second != null ->
                string(Strings.PluginDialogLabelMinMax, range.first, range.second)
            range.first != null ->
                string(Strings.PluginDialogLabelMin, range.first)
            range.second != null ->
                string(Strings.PluginDialogLabelMax, range.second)
            else -> ""
        }
        return label + suffix
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
}

@Composable
private fun rememberState(
    plugin: Plugin,
    paramMap: ParamMap,
    submit: (ParamMap?) -> Unit
) = remember { PluginDialogState(plugin, paramMap, submit) }

@Composable
fun PluginDialog(
    plugin: Plugin,
    paramMap: ParamMap,
    submit: (ParamMap?) -> Unit,
    state: PluginDialogState = rememberState(plugin, paramMap, submit)
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
                Text(
                    text = plugin.displayedName,
                    style = MaterialTheme.typography.h4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    text = string(Strings.PluginDialogInfo, plugin.author, plugin.version),
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                if (plugin.description.isNotBlank()) {
                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.caption,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
private fun Params(state: PluginDialogState) {
    Column(
        modifier = Modifier.background(color = White20, shape = RoundedCornerShape(10.dp))
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        state.params.indices.forEach { i ->
            Row(Modifier.heightIn(min = 60.dp)) {
                Text(
                    text = state.getLabel(i),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.width(300.dp).align(Alignment.CenterVertically),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
