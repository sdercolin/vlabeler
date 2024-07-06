package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.json
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
import java.io.File

/**
 * An interface for all "plugins", including [Plugin]s and [LabelerConf]s.
 */
interface BasePlugin {
    val name: String
    val version: Int
    val displayedName: LocalizedJsonString
    val description: LocalizedJsonString
    val author: String
    val email: String
    val website: String
    val directory: File?
    val parameterDefs: List<Parameter<*>>
    val resourceFiles: List<String>
    val isSelfExecutable: Boolean
        get() = false

    /**
     * Save the given [paramMap] to the given [file].
     */
    suspend fun saveParams(paramMap: ParamMap, file: File) = withContext(Dispatchers.IO) {
        val contentText = stringifyParams(paramMap)
        Log.debug("Saving params to ${file.absolutePath}")
        file.writeText(contentText)
    }

    /**
     * Stringify the given [paramMap] to a JSON string.
     */
    fun stringifyParams(paramMap: ParamMap): String {
        val content = paramMap.mapValues { (_, value) ->
            when (value) {
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is EntrySelector -> json.encodeToJsonElement(value)
                is FileWithEncoding -> json.encodeToJsonElement(value)
                else -> throw IllegalArgumentException("`$value` is not a supported value")
            }
        }
        return JsonObject(content).toString()
    }

    /**
     * Get the default parameters.
     */
    fun getDefaultParams() = parameterDefs.associate { parameter ->
        parameter.name to requireNotNull(parameter.defaultValue)
    }.toParamMap()

    /**
     * Load the saved parameters from the given [file].
     */
    suspend fun loadSavedParams(file: File): ParamMap = withContext(Dispatchers.IO) {
        runCatching {
            file.takeIf { it.exists() }
                ?.readText()
                ?.let { parseParamMap(it) }
                ?: getDefaultParams()
        }
            .onFailure { Log.debug("Failed to load saved params from file ${file.absolutePath}") }
            .getOrElse { getDefaultParams() }
    }

    suspend fun loadSavedParamsJson(file: File): String = withContext(Dispatchers.IO) {
        runCatching {
            file.takeIf { it.exists() }
                ?.readText()
                ?: stringifyParams(getDefaultParams())
        }
            .onFailure { Log.debug("Failed to load saved params from file ${file.absolutePath}") }
            .getOrElse { stringifyParams(getDefaultParams()) }
    }

    /**
     * Parse Json content to a [ParamMap].
     */
    fun parseParamMap(contentText: String): ParamMap {
        val jsonObject = json.parseToJsonElement(contentText).jsonObject
        return parameterDefs.associate {
            val value = jsonObject[it.name]
                ?.let { element ->
                    when (it) {
                        is Parameter.IntParam -> element.jsonPrimitive.int
                        is Parameter.FloatParam -> element.jsonPrimitive.float
                        is Parameter.BooleanParam -> element.jsonPrimitive.boolean
                        is Parameter.EntrySelectorParam -> json.decodeFromJsonElement<EntrySelector>(
                            element,
                        )
                        is Parameter.EnumParam -> element.jsonPrimitive.content
                        is Parameter.FileParam -> json.decodeFromJsonElement<FileWithEncoding>(element)
                        is Parameter.StringParam -> element.jsonPrimitive.content
                        is Parameter.RawFileParam -> element.jsonPrimitive.content
                    }
                }
                ?: requireNotNull(it.defaultValue)
            it.name to value
        }.toParamMap()
    }

    /**
     * Check if the given [params] are valid.
     */
    fun checkParams(params: ParamMap, labelerConf: LabelerConf?): Boolean =
        parameterDefs.all { param ->
            val value = params[param.name] ?: return@all false
            param.check(value, labelerConf)
        }

    /**
     * Get the [File] to save the parameters.
     */
    fun getSavedParamsFile(): File

    /**
     * Get resource file contents as a list of strings.
     */
    fun readResourceFiles() = resourceFiles.map { requireNotNull(directory).resolve(it).readText() }
}
