package com.sdercolin.vlabeler.ui.dialog.customization

import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppState

class MacroPluginItem(
    private val plugin: Plugin,
    private val appState: AppState,
    disabled: Boolean,
) : CustomizableItem(
    name = plugin.name,
    author = plugin.author,
    version = plugin.version,
    displayedName = plugin.displayedName,
    description = plugin.description,
    email = plugin.email,
    website = plugin.website,
    rootFile = requireNotNull(plugin.directory),
    canRemove = plugin.builtIn.not(),
    disabled = disabled,
) {

    override fun canExecute(): Boolean {
        return plugin.isMacroExecutable(appState)
    }

    override fun execute() {
        appState.openMacroPluginDialog(plugin)
    }
}
