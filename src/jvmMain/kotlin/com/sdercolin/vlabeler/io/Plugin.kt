package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultPluginDir
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.parseJson
import java.io.File

/**
 * Load plugins from the plugin directory.
 *
 * @param type The type of the plugin.
 * @param language The current language. It's only used for sorting the plugins.
 */
fun loadPlugins(type: Plugin.Type, language: Language): List<Plugin> =
    listOf(CustomPluginDir, DefaultPluginDir)
        .let { if (isDebug) it.reversed() else it }
        .flatMap { it.resolve(type.directoryName).getChildren() }
        .asSequence()
        .filter { it.isDirectory }
        .distinctBy { it.name }
        .map { it.resolve(PLUGIN_INFO_FILE_NAME) }
        .filter { it.exists() }
        .map { it to it.readText() }
        .mapNotNull { (file, text) ->
            runCatching { text.parseJson<Plugin>().validate() }.getOrElse {
                Log.debug(it)
                Log.debug("Failed to load plugin: ${file.parent}")
                null
            }?.let { plugin ->
                Log.info("Loaded plugin: ${file.parent}")
                val isBuiltIn = file.absolutePath.contains(DefaultPluginDir.absolutePath)
                val parametersInjected = plugin.parameters?.list?.let { list ->
                    val newList = list.map { param ->
                        when (param) {
                            is Parameter.StringParam -> {
                                val fileNameMatched = Parameter.StringParam.DefaultValueFileReferencePattern
                                    .find(param.defaultValue)?.groupValues?.getOrNull(1)
                                if (fileNameMatched != null) {
                                    val content = file.parentFile.resolve(fileNameMatched).readText().trim()
                                    param.copy(defaultValue = content)
                                } else {
                                    param
                                }
                            }
                            is Parameter.FileParam -> {
                                if (param.optional.not() || param.defaultValue.file?.isNotEmpty() == true) {
                                    val defaultValue = param.defaultValue.let {
                                        it.copy(
                                            file = it.file?.resolveRelativePath(file.parentFile),
                                        )
                                    }
                                    param.copy(defaultValue = defaultValue)
                                } else {
                                    param
                                }
                            }
                            is Parameter.RawFileParam -> {
                                if (param.optional.not() || param.defaultValue.isNotEmpty()) {
                                    val defaultValue = param.defaultValue.resolveRelativePath(file.parentFile)
                                    param.copy(defaultValue = defaultValue)
                                } else {
                                    param
                                }
                            }
                            else -> param
                        }
                    }
                    Plugin.Parameters(list = newList)
                }
                plugin.copy(
                    directory = file.parentFile,
                    builtIn = isBuiltIn,
                    parameters = parametersInjected,
                )
            }
        }
        .sortedBy { it.displayedName.getCertain(language) }
        .toList()

private fun String.resolveRelativePath(parent: File): String {
    return parent.resolve(this).absolutePath
}

const val PLUGIN_INFO_FILE_NAME = "plugin.json"
