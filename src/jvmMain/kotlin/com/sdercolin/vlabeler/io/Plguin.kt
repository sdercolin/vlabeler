package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.util.DefaultPluginDir
import com.sdercolin.vlabeler.util.parseJson

fun loadPlugins(type: Plugin.Type): List<Plugin> =
    DefaultPluginDir.resolve(type.directoryName).listFiles()
        .orEmpty()
        .filter { it.isDirectory }
        .map { it.resolve(PluginInfoFileName) }
        .filter { it.exists() }
        .map { it to it.readText() }
        .mapNotNull { (file, text) ->
            runCatching { parseJson<Plugin>(text) }.getOrElse {
                Log.debug(it)
                Log.debug("Failed to load plugin: ${file.parent}")
                null
            }?.let {
                Log.info("Loaded plugin: ${file.parent}")
                it.copy(directory = file.parentFile)
            }
        }

private const val PluginInfoFileName = "plugin.json"
