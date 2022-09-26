package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.FileWithEncoding
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            runCatching { text.parseJson<Plugin>() }.getOrElse {
                Log.debug(it)
                Log.debug("Failed to load plugin: ${file.parent}")
                null
            }?.let { plugin ->
                Log.info("Loaded plugin: ${file.parent}")
                val isBuiltIn = file.absolutePath.contains(DefaultPluginDir.absolutePath)
                val parametersInjectedWithFileContents = plugin.parameters?.list?.let { list ->
                    val newList = list.map { param ->
                        if (param is Plugin.Parameter.StringParam) {
                            val fileNameMatched = Plugin.Parameter.StringParam.DefaultValueFileReferencePattern
                                .find(param.defaultValue)?.groupValues?.getOrNull(1)
                            if (fileNameMatched != null) {
                                val content = file.parentFile.resolve(fileNameMatched).readText().trim()
                                Plugin.Parameter.StringParam(
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

suspend fun Plugin.loadSavedParams(): ParamMap = withContext(Dispatchers.IO) {
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .takeIf { it.exists() }
        ?.readText()
        ?.let { contentText ->
            val jsonObject = json.parseToJsonElement(contentText).jsonObject
            parameters?.list.orEmpty().associate {
                val value = jsonObject[it.name]
                    ?.let { element ->
                        when (it) {
                            is Plugin.Parameter.IntParam -> element.jsonPrimitive.int
                            is Plugin.Parameter.FloatParam -> element.jsonPrimitive.float
                            is Plugin.Parameter.BooleanParam -> element.jsonPrimitive.boolean
                            is Plugin.Parameter.EntrySelectorParam -> json.decodeFromJsonElement<EntrySelector>(element)
                            is Plugin.Parameter.EnumParam -> json.decodeFromJsonElement<LocalizedJsonString>(element)
                            is Plugin.Parameter.FileParam -> json.decodeFromJsonElement<FileWithEncoding>(element)
                            is Plugin.Parameter.StringParam -> element.jsonPrimitive.content
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
            is LocalizedJsonString -> json.encodeToJsonElement(value)
            is FileWithEncoding -> json.encodeToJsonElement(value)
            else -> throw IllegalArgumentException("`$value` is not a supported value")
        }
    }
    val contentText = JsonObject(content).toString()
    requireNotNull(directory).resolve(PluginSavedParamsFileName)
        .writeText(contentText)
}

const val PluginInfoFileName = "plugin.json"
private const val PluginSavedParamsFileName = ".saved.json"
