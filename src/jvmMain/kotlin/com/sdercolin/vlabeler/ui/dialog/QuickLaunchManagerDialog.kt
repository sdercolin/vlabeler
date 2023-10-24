package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.PluginQuickLaunch
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.HeaderFooterColumn
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import com.sdercolin.vlabeler.ui.dialog.plugin.MacroPluginDialog
import com.sdercolin.vlabeler.ui.dialog.plugin.MacroPluginDialogArgs
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditorState
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap

@Composable
private fun rememberState(appState: AppState) = remember(appState) {
    QuickLaunchManagerDialogState(appState)
}

class QuickLaunchManagerDialogState(val appState: AppState) {

    private var slots by mutableStateOf(
        (0 until PluginQuickLaunch.SLOT_COUNT).map { appState.appRecordStore.value.getPluginQuickLaunch(it) },
    )

    val appConf get() = appState.appConf
    val appRecordStore get() = appState.appRecordStore
    val project get() = appState.project

    fun getPluginOptions(): List<Plugin?> = listOf(null) + appState.getPlugins(Plugin.Type.Macro)

    fun getSlot(slot: Int): PluginQuickLaunch? = slots.getOrNull(slot)

    fun findPlugin(quickLaunch: PluginQuickLaunch) =
        appState.getPlugins(Plugin.Type.Macro).find { it.name == quickLaunch.pluginName }

    fun setSlotPlugin(slot: Int, plugin: Plugin?) {
        val existing = slots[slot]
        if (plugin === null) {
            save(slot, null)
            return
        }
        val new = if (existing == null) {
            PluginQuickLaunch(plugin.name, params = null, skipDialog = false)
        } else {
            if (existing.pluginName == plugin.name) {
                return
            }
            existing.copy(pluginName = plugin.name, params = null)
        }
        save(slot, new)
    }

    fun setSlotParams(slot: Int, plugin: Plugin, params: ParamMap) {
        val existing = slots[slot] ?: return
        val new = existing.copy(params = ParamTypedMap.from(params, plugin.parameterDefs))
        save(slot, new)
    }

    fun setSlotSkipDialog(slot: Int, skipDialog: Boolean) {
        val existing = slots[slot] ?: return
        val new = existing.copy(skipDialog = skipDialog)
        save(slot, new)
    }

    private fun save(slot: Int, quickLaunch: PluginQuickLaunch?) {
        slots = slots.toMutableList().apply { set(slot, quickLaunch) }
        appState.appRecordStore.update { saveQuickLaunch(slot, quickLaunch) }
    }

    fun finish() {
        appState.closeQuickLaunchManagerDialog()
    }

    fun openKeymap() {
        appState.openPreferencesDialog(PreferencesEditorState.LaunchArgs.Keymap("Launch Plugin Slot"))
    }
}

@Composable
fun QuickLaunchManagerDialog(appState: AppState, state: QuickLaunchManagerDialogState = rememberState(appState)) {
    LargeDialogContainer(wrapHeight = true) {
        HeaderFooterColumn(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 40.dp),
            scrollbarWidth = 15.dp,
            scrollbarSpacing = (-15).dp,
            header = {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = string(Strings.QuickLaunchManagerDialogTitle),
                    style = MaterialTheme.typography.h5,
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = string(Strings.QuickLaunchManagerDialogDescription),
                    style = MaterialTheme.typography.body2,
                )
                Spacer(modifier = Modifier.height(25.dp))
                TableHeaderRow()
            },
            footer = {
                Spacer(modifier = Modifier.height(25.dp))
                BottomButtonBar(state)
            },
        ) {
            Content(state)
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(100.dp)) {
            Text(
                text = string(Strings.QuickLaunchManagerDialogHeaderTitle),
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = string(Strings.QuickLaunchManagerDialogHeaderPlugin),
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Box(modifier = Modifier.width(150.dp)) {
            Text(
                text = string(Strings.QuickLaunchManagerDialogHeaderForceAskParams),
                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Content(state: QuickLaunchManagerDialogState) {
    repeat(PluginQuickLaunch.SLOT_COUNT) { index ->
        Item(index, state)
    }
}

@Composable
private fun Item(index: Int, state: QuickLaunchManagerDialogState) {
    val savedQuickLaunch = state.getSlot(index)
    var quickLaunch by remember(savedQuickLaunch) { mutableStateOf(savedQuickLaunch) }
    val plugin = quickLaunch?.let { state.findPlugin(it) }
    var isPluginDialogShown by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(100.dp)) {
            Text(
                text = string(Strings.QuickLaunchManagerDialogItemTitle, index + 1),
                style = MaterialTheme.typography.body1,
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            val text = quickLaunch?.let {
                plugin?.displayedName?.get() ?: it.pluginName
            }.orEmpty()
            val color = if (quickLaunch != null && plugin == null) {
                MaterialTheme.colors.error
            } else {
                MaterialTheme.colors.onSurface
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = text,
                    onValueChange = { },
                    textStyle = MaterialTheme.typography.body1.copy(color = color),
                    readOnly = true,
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ExpandMore, null)
                        }
                    },
                )
                Spacer(modifier = Modifier.width(15.dp))
                IconButton(
                    enabled = plugin != null,
                    onClick = { isPluginDialogShown = true },
                ) {
                    Icon(Icons.Default.Settings, null)
                }
            }
            DropdownMenu(
                modifier = Modifier.align(Alignment.CenterEnd),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                state.getPluginOptions().forEach { plugin ->
                    DropdownMenuItem(
                        onClick = {
                            state.setSlotPlugin(index, plugin)
                            expanded = false
                        },
                    ) {
                        Text(text = plugin?.displayedName?.get().orEmpty())
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Box(modifier = Modifier.width(150.dp)) {
            Switch(
                modifier = Modifier.align(Alignment.CenterStart),
                checked = quickLaunch?.skipDialog == false,
                onCheckedChange = { state.setSlotSkipDialog(index, !it) },
                colors = getSwitchColors(),
                enabled = plugin != null,
            )
        }
    }
    if (isPluginDialogShown) {
        if (plugin != null) {
            val snackbarHostState = remember { SnackbarHostState() }
            MacroPluginDialog(
                appConf = state.appConf,
                appRecordStore = state.appRecordStore,
                snackbarHostState = snackbarHostState,
                args = MacroPluginDialogArgs(
                    plugin = plugin,
                    paramMap = requireNotNull(quickLaunch).getMergedParams(plugin),
                    slot = index,
                ),
                project = state.project,
                save = { params ->
                    state.setSlotParams(index, plugin, params)
                },
                load = {
                    quickLaunch = requireNotNull(quickLaunch).copy(
                        params = ParamTypedMap.from(it, plugin.parameterDefs),
                    )
                },
                submit = { params ->
                    if (params != null) {
                        state.setSlotParams(index, plugin, params)
                    }
                    isPluginDialogShown = false
                },
                executable = false,
            )
        } else {
            isPluginDialogShown = false
        }
    }
}

@Composable
private fun BottomButtonBar(state: QuickLaunchManagerDialogState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = state::openKeymap) {
            Text(text = string(Strings.QuickLaunchManagerDialogOpenKeymap))
        }
        Spacer(modifier = Modifier.weight(1f))
        ConfirmButton(onClick = state::finish)
    }
}
