package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings

class MacroPluginManagerDialogState(
    items: List<MacroPluginItem>,
    appState: AppState,
    appRecordStore: AppRecordStore
) : PluginManagerDialogState<MacroPluginItem>(
    pluginType = Plugin.Type.Macro,
    items = items,
    title = Strings.MacroPluginManagerTitle,
    allowExecution = true,
    appState = appState,
    appRecordStore = appRecordStore
)

@Composable
fun rememberMacroPluginManagerState(appState: AppState): MacroPluginManagerDialogState {
    val plugins = appState.getPlugins(Plugin.Type.Macro)
    return remember(plugins) {
        val items = plugins.map {
            MacroPluginItem(
                plugin = it,
                disabled = appState.appRecordStore.stateFlow.value.disabledPluginNames.contains(it.name)
            )
        }
        MacroPluginManagerDialogState(
            items = items,
            appState = appState,
            appRecordStore = appState.appRecordStore
        )
    }
}
