@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Only deserialization is supported
 * See [readme/plugin-development.md] for more information
 */
@Serializable
@Immutable
data class Plugin(
    val name: String,
    val version: Int = 1,
    val type: Type,
    val displayedName: String = name,
    val author: String,
    val email: String = "",
    val description: String = "",
    val website: String = "",
    val supportedLabelFileExtension: String,
    val inputFileExtension: String? = null,
    val requireInputFile: Boolean = false,
    val allowMultipleInputFiles: Boolean = false,
    val outputRawEntry: Boolean = false,
    val parameters: Parameters? = null,
    val scriptFiles: List<String>,
    val resourceFiles: List<String> = listOf(),
    @Transient val directory: File? = null,
    @Transient val builtIn: Boolean = false
) {
    fun readResourceFiles() = resourceFiles.map { requireNotNull(directory).resolve(it).readText() }
    fun readScriptTexts() = scriptFiles.map { requireNotNull(directory).resolve(it).readText() }
    fun getDefaultParams() = parameters?.list.orEmpty().associate { parameter ->
        parameter.name to requireNotNull(parameter.defaultValue)
    }.toParamMap()

    fun isMacroExecutable(appState: AppState): Boolean {
        if (appState.isMacroPluginAvailable.not()) return false
        if (isLabelFileExtensionSupported(appState.requireProject().labelerConf.extension).not()) return false
        return true
    }

    fun isLabelFileExtensionSupported(extension: String) =
        supportedLabelFileExtension == "*" || supportedLabelFileExtension.split('|').contains(extension)

    @Serializable
    enum class Type(val directoryName: String) {
        @SerialName("template")
        Template("template"),

        @SerialName("macro")
        Macro("macro")
    }

    @Serializable(PluginParameterListSerializer::class)
    class Parameters(
        val list: List<Parameter<*>>
    )

    @Serializable(PluginParameterSerializer::class)
    @Immutable
    sealed class Parameter<T : Any>(
        val type: ParameterType,
        val name: String,
        val label: String,
        val description: String?,
        open val defaultValue: T,
    ) {
        class IntParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: Int,
            val min: Int?,
            val max: Int?
        ) : Parameter<Int>(ParameterType.Integer, name, label, description, defaultValue)

        class FloatParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: Float,
            val min: Float?,
            val max: Float?
        ) : Parameter<Float>(ParameterType.Float, name, label, description, defaultValue)

        class BooleanParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: Boolean
        ) : Parameter<Boolean>(ParameterType.Boolean, name, label, description, defaultValue)

        class StringParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: String,
            val multiLine: Boolean,
            val optional: Boolean
        ) : Parameter<String>(ParameterType.String, name, label, description, defaultValue) {

            companion object {
                val DefaultValueFileReferencePattern = Regex("^file::(.*)$")
                fun getDefaultValueFromFile(fileName: String) = "file::$fileName"
            }
        }

        class EnumParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: String,
            val options: List<String>
        ) : Parameter<String>(ParameterType.Enum, name, label, description, defaultValue)

        class EntrySelectorParam(
            name: String,
            label: String,
            description: String?,
            defaultValue: EntrySelector
        ) : Parameter<EntrySelector>(ParameterType.EntrySelector, name, label, description, defaultValue)
    }

    @Serializable
    enum class ParameterType {
        @SerialName("integer")
        Integer,

        @SerialName("float")
        Float,

        @SerialName("boolean")
        Boolean,

        @SerialName("string")
        String,

        @SerialName("enum")
        Enum,

        @SerialName("entrySelector")
        EntrySelector
    }

    fun checkParams(params: ParamMap, labelerConf: LabelerConf?): Boolean {
        return parameters?.list.orEmpty().all { param ->
            when (param) {
                is Parameter.BooleanParam -> (params[param.name] as? Boolean) != null
                is Parameter.EnumParam -> (params[param.name] as? String)?.let { enumValue ->
                    enumValue in param.options
                } == true
                is Parameter.FloatParam -> (params[param.name] as? Float)?.let { floatValue ->
                    floatValue >= (param.min ?: Float.NEGATIVE_INFINITY) &&
                        floatValue <= (param.max ?: Float.POSITIVE_INFINITY)
                } == true
                is Parameter.IntParam -> (params[param.name] as? Int)?.let { intValue ->
                    intValue >= (param.min ?: Int.MIN_VALUE) && intValue <= (param.max ?: Int.MIN_VALUE)
                } == true
                is Parameter.StringParam -> (params[param.name] as? String)?.let { stringValue ->
                    if (param.optional.not()) stringValue.isNotEmpty() else true
                } == true
                is Parameter.EntrySelectorParam -> (params[param.name] as? EntrySelector)?.let { selector ->
                    selector.filters.all { it.isValid(requireNotNull(labelerConf)) }
                } == true
            }
        }
    }
}

@Serializer(Plugin.Parameters::class)
object PluginParameterListSerializer : KSerializer<Plugin.Parameters> {
    override fun deserialize(decoder: Decoder): Plugin.Parameters {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        val list = requireNotNull(element["list"]).jsonArray.map {
            decoder.json.decodeFromJsonElement(PluginParameterSerializer, it)
        }
        return Plugin.Parameters(list)
    }
}

@Serializer(Plugin.Parameter::class)
object PluginParameterSerializer : KSerializer<Plugin.Parameter<*>> {
    override fun deserialize(decoder: Decoder): Plugin.Parameter<*> {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        val type = requireNotNull(element["type"]).jsonPrimitive.content.parseJson<Plugin.ParameterType>()
        val name = requireNotNull(element["name"]).jsonPrimitive.content
        val label = requireNotNull(element["label"]).jsonPrimitive.content
        val description = element["description"]?.jsonPrimitive?.content
        val defaultValue = requireNotNull(element["defaultValue"])
        return when (type) {
            Plugin.ParameterType.Integer -> {
                val default = defaultValue.jsonPrimitive.int
                val min = element["min"]?.jsonPrimitive?.int
                val max = element["max"]?.jsonPrimitive?.int
                Plugin.Parameter.IntParam(name, label, description, default, min, max)
            }
            Plugin.ParameterType.Float -> {
                val default = defaultValue.jsonPrimitive.float
                val min = element["min"]?.jsonPrimitive?.float
                val max = element["max"]?.jsonPrimitive?.float
                Plugin.Parameter.FloatParam(name, label, description, default, min, max)
            }
            Plugin.ParameterType.Boolean -> {
                val default = defaultValue.jsonPrimitive.boolean
                Plugin.Parameter.BooleanParam(name, label, description, default)
            }
            Plugin.ParameterType.String -> {
                val defaultValueFromFile = element["defaultValueFromFile"]?.jsonPrimitive?.content
                val default = defaultValueFromFile
                    ?.let(Plugin.Parameter.StringParam::getDefaultValueFromFile)
                    ?: defaultValue.jsonPrimitive.content
                val optional = requireNotNull(element["optional"]).jsonPrimitive.boolean
                val multiLine = element["multiLine"]?.jsonPrimitive?.boolean ?: false
                Plugin.Parameter.StringParam(name, label, description, default, multiLine, optional)
            }
            Plugin.ParameterType.Enum -> {
                val default = defaultValue.jsonPrimitive.content
                val options = requireNotNull(element["options"]).jsonArray.map {
                    it.jsonPrimitive.content
                }
                Plugin.Parameter.EnumParam(name, label, description, default, options)
            }
            Plugin.ParameterType.EntrySelector -> {
                val default = decoder.json.decodeFromJsonElement(EntrySelector.serializer(), defaultValue)
                Plugin.Parameter.EntrySelectorParam(name, label, description, default)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Plugin.Parameter<*>) {
        // nop
    }
}
