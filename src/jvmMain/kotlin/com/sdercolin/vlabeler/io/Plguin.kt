package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.util.CustomPluginDir
import com.sdercolin.vlabeler.util.DefaultPluginDir
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.json
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun loadPlugins(type: Plugin.Type): List<Plugin> =
    listOf(CustomPluginDir, DefaultPluginDir)
        .let { if (isDebug) it.reversed() else it }
        .flatMap { it.resolve(type.directoryName).getChildren() }
        .filter { it.isDirectory }
        .distinctBy { it.name }
        .map { it.resolve(PluginInfoFileName) }
        .filter { it.exists() }
        .map { it to it.readText() }
        .mapNotNull { (file, text) ->
            runCatching { text.parseJson<Plugin>() }.getOrElse {
                Log.debug(it)
                Log.debug("Failed to load plugin: ${file.parent}")
                null
            }?.let {
                Log.info("Loaded plugin: ${file.parent}")
                val isBuiltIn = file.absolutePath.contains(DefaultPluginDir.absolutePath)
                it.copy(directory = file.parentFile, builtIn = isBuiltIn)
            }
        }

suspend fun Plugin.loadSavedParams(): ParamMap = withContext(Dispatchers.IO) {
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .takeIf { it.exists() }
        ?.readText()
        ?.let { contentText ->
            val jsonObject = json.parseToJsonElement(contentText).jsonObject
            parameters?.list.orEmpty().associate {
                val value = jsonObject[it.name]
                    ?.let { element ->
                        when (it.type) {
                            Plugin.ParameterType.Integer -> element.jsonPrimitive.int
                            Plugin.ParameterType.Float -> element.jsonPrimitive.float
                            Plugin.ParameterType.Boolean -> element.jsonPrimitive.boolean
                            Plugin.ParameterType.EntrySelector -> json.decodeFromJsonElement(
                                EntrySelector.serializer(),
                                element
                            )
                            else -> element.jsonPrimitive.content
                        }
                    }
                    ?: requireNotNull(it.defaultValue)
                it.name to value
            }
        }
        ?.toParamMap()
        ?: getDefaultParams()
}

suspend fun Plugin.saveParams(paramMap: ParamMap) = withContext(Dispatchers.IO) {
    val content = paramMap.mapValues { (_, value) ->
        when (value) {
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is EntrySelector -> json.encodeToJsonElement(value)
            else -> throw IllegalArgumentException("`$value` is not a supported value")
        }
    }
    val contentText = JsonObject(content).toString()
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .writeText(contentText)
}

private const val PluginInfoFileName = "plugin.json"
private const val PluginSavedParamsFileName = ".saved.json"
