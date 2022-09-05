package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings

class MacroPluginManagerDialogState(
    appState: AppState,
    appRecordStore: AppRecordStore,
) : PluginManagerDialogState<MacroPluginItem>(
    pluginType = Plugin.Type.Macro,
    title = Strings.MacroPluginManagerTitle,
    importDialogTitle = Strings.MacroPluginManagerImportDialogTitle,
    allowExecution = true,
    appState = appState,
    appRecordStore = appRecordStore,
)

@Composable
fun rememberMacroPluginManagerState(appState: AppState): MacroPluginManagerDialogState {
    val plugins = appState.getPlugins(Plugin.Type.Macro)
    val state = remember {
        MacroPluginManagerDialogState(
            appState = appState,
            appRecordStore = appState.appRecordStore,
        )
    }
    LaunchedEffect(plugins) {
        val items = plugins.map {
            MacroPluginItem(
                plugin = it,
                appState = appState,
                disabled = appState.appRecordStore.value.disabledPluginNames.contains(it.name),
            )
        }
        state.loadItems(items)
    }
    return state
}
