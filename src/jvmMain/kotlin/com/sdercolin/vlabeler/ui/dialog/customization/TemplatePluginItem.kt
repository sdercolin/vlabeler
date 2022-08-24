package com.sdercolin.vlabeler.ui.dialog.customization

import com.sdercolin.vlabeler.model.Plugin

class TemplatePluginItem(
    plugin: Plugin,
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
)
