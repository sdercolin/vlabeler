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

fun loadPlugins(type: Plugin.Type, language: Language): List<Plugin> =
    listOf(CustomPluginDir, DefaultPluginDir)
        .let { if (isDebug) it.reversed() else it }
        .flatMap { it.resolve(type.directoryName).getChildren() }
        .filter { it.isDirectory }
        .distinctBy { it.name }
        .map { it.resolve(PluginInfoFileName) }
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
                val parametersInjectedWithFileContents = plugin.parameters?.list?.let { list ->
                    val newList = list.map { param ->
                        if (param is Parameter.StringParam) {
                            val fileNameMatched = Parameter.StringParam.DefaultValueFileReferencePattern
                                .find(param.defaultValue)?.groupValues?.getOrNull(1)
                            if (fileNameMatched != null) {
                                val content = file.parentFile.resolve(fileNameMatched).readText().trim()
                                Parameter.StringParam(
                                    name = param.name,
                                    label = param.label,
                                    description = param.description,
                                    enableIf = param.enableIf,
                                    defaultValue = content,
                                    multiLine = param.multiLine,
                                    optional = param.optional,
                                )
                            } else {
                                param
                            }
                        } else {
                            param
                        }
                    }
                    Plugin.Parameters(list = newList)
                }
                plugin.copy(
                    directory = file.parentFile,
                    builtIn = isBuiltIn,
                    parameters = parametersInjectedWithFileContents,
                )
            }
        }
        .sortedBy { it.displayedName.getCertain(language) }

const val PluginInfoFileName = "plugin.json"
