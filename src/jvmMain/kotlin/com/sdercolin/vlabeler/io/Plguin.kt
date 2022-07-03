package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.util.DefaultPluginDir
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.json
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

suspend fun Plugin.loadSavedParams(): ParamMap = withContext(Dispatchers.IO) {
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .takeIf { it.exists() }
        ?.readText()
        ?.let { contentText ->
            val jsonObject = json.parseToJsonElement(contentText).jsonObject
            parameters?.list.orEmpty().associate {
                val value = jsonObject[it.name]
                    ?.let { literal ->
                        when (it.type) {
                            Plugin.ParameterType.Integer -> literal.jsonPrimitive.int
                            Plugin.ParameterType.Float -> literal.jsonPrimitive.float
                            Plugin.ParameterType.Boolean -> literal.jsonPrimitive.boolean
                            else -> literal.jsonPrimitive.content
                        }
                    }
                    ?: requireNotNull(it.defaultValue)
                it.name to value
            }
        }
        ?: getDefaultParams()
}

suspend fun Plugin.saveParams(paramMap: ParamMap) = withContext(Dispatchers.IO) {
    val content = paramMap.mapValues { (_, value) ->
        when (value) {
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> throw IllegalArgumentException("`$value` is not a supported JsonPrimitive value")
        }
    }
    val contentText = JsonObject(content).toString()
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .writeText(contentText)
}

private const val PluginInfoFileName = "plugin.json"
private const val PluginSavedParamsFileName = ".saved.json"
