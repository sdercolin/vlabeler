package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings

class TemplatePluginManagerDialogState(
    appState: AppState,
    appRecordStore: AppRecordStore,
) : PluginManagerDialogState<TemplatePluginItem>(
    pluginType = Plugin.Type.Template,
    title = Strings.TemplatePluginManagerTitle,
    importDialogTitle = Strings.TemplatePluginManagerImportDialogTitle,
    allowExecution = false,
    appState = appState,
    appRecordStore = appRecordStore,
)

@Composable
fun rememberTemplatePluginManagerState(appState: AppState): TemplatePluginManagerDialogState {
    val plugins = appState.getPlugins(Plugin.Type.Template)
    val state = remember {
        TemplatePluginManagerDialogState(
            appState = appState,
            appRecordStore = appState.appRecordStore,
        )
    }
    LaunchedEffect(plugins) {
        val items = plugins.map {
            TemplatePluginItem(
                plugin = it,
                disabled = appState.appRecordStore.value.disabledPluginNames.contains(it.name),
            )
        }
        state.loadItems(items)
    }
    return state
}
