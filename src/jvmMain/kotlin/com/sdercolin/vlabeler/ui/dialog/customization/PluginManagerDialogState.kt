package com.sdercolin.vlabeler.ui.dialog.customization

import com.sdercolin.vlabeler.exception.CustomizableItemLoadingException
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.parseJson
import java.io.File

abstract class PluginManagerDialogState<T : CustomizableItem>(
    private val pluginType: Plugin.Type,
    title: Strings,
    importDialogTitle: Strings,
    allowExecution: Boolean,
    appState: AppState,
    appRecordStore: AppRecordStore,
) : CustomizableItemManagerDialogState<T>(
    title = title,
    importDialogTitle = importDialogTitle,
    definitionFileExtension = "json",
    directory = getPluginsDirectory(pluginType),
    allowExecution = allowExecution,
    appState = appState,
    appRecordStore = appRecordStore,
) {
    override fun saveDisabled(index: Int) {
        val item = items[index]
        appRecordStore.update { setPluginDisabled(item.name, item.disabled) }
    }

    override suspend fun importNewItem(configFile: File) = runCatching {
        val plugin = configFile.readText().parseJson<Plugin>()
        require(plugin.type == pluginType) { "Unexpected Plugin type: ${plugin.type}" }
        val targetFolder = getPluginsDirectory(pluginType).resolve(plugin.name)
        require(configFile.parentFile.copyRecursively(targetFolder, overwrite = true)) {
            "Failed to copy plugin to ${targetFolder.absolutePath}"
        }
        plugin.name
    }.getOrElse {
        throw CustomizableItemLoadingException(it)
    }

    override fun reload() {
        appState.reloadPlugins()
    }

    companion object {
        private fun getPluginsDirectory(type: Plugin.Type) = CustomPluginDir.resolve(type.directoryName)
    }
}
