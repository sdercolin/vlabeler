@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.json
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
import kotlinx.serialization.json.decodeFromJsonElement
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
    val displayedName: LocalizedJsonString = name.toLocalized(),
    val author: String,
    val email: String = "",
    val description: LocalizedJsonString = "".toLocalized(),
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
    @Transient val builtIn: Boolean = false,
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
        Macro("macro"),
    }

    @Serializable(PluginParameterListSerializer::class)
    class Parameters(
        val list: List<Parameter<*>>,
    )

    @Serializable(PluginParameterSerializer::class)
    @Immutable
    sealed class Parameter<T : Any>(
        val type: ParameterType,
        val name: String,
        val label: LocalizedJsonString,
        val description: LocalizedJsonString?,
        val enableIf: String?,
        open val defaultValue: T,
    ) {
        abstract fun eval(value: Any): Boolean

        class IntParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: Int,
            val min: Int?,
            val max: Int?,
        ) : Parameter<Int>(ParameterType.Integer, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is Int && value != 0
        }

        class FloatParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: Float,
            val min: Float?,
            val max: Float?,
        ) : Parameter<Float>(ParameterType.Float, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is Float && value != 0f && value.isNaN().not()
        }

        class BooleanParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: Boolean,
        ) : Parameter<Boolean>(ParameterType.Boolean, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is Boolean && value
        }

        class StringParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: String,
            val multiLine: Boolean,
            val optional: Boolean,
        ) : Parameter<String>(ParameterType.String, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is String && value.isNotEmpty()

            companion object {
                val DefaultValueFileReferencePattern = Regex("^file::(.*)$")
                fun getDefaultValueFromFile(fileName: String) = "file::$fileName"
            }
        }

        class EnumParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: LocalizedJsonString,
            val options: List<LocalizedJsonString>,
        ) : Parameter<LocalizedJsonString>(ParameterType.Enum, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is LocalizedJsonString
        }

        class EntrySelectorParam(
            name: String,
            label: LocalizedJsonString,
            description: LocalizedJsonString?,
            enableIf: String?,
            defaultValue: EntrySelector,
        ) : Parameter<EntrySelector>(ParameterType.EntrySelector, name, label, description, enableIf, defaultValue) {

            override fun eval(value: Any) = value is EntrySelector && value.filters.isNotEmpty()
        }
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
        EntrySelector,
    }

    fun checkParams(params: ParamMap, labelerConf: LabelerConf?): Boolean {
        return parameters?.list.orEmpty().all { param ->
            when (param) {
                is Parameter.BooleanParam -> (params[param.name] as? Boolean) != null
                is Parameter.EnumParam -> (params[param.name] as? LocalizedJsonString)?.let { enumValue ->
                    enumValue in param.options
                } == true
                is Parameter.FloatParam -> (params[param.name] as? Float)?.let { floatValue ->
                    floatValue >= (param.min ?: Float.NEGATIVE_INFINITY) &&
                        floatValue <= (param.max ?: Float.POSITIVE_INFINITY)
                } == true
                is Parameter.IntParam -> (params[param.name] as? Int)?.let { intValue ->
                    intValue >= (param.min ?: Int.MIN_VALUE) && intValue <= (param.max ?: Int.MAX_VALUE)
                } == true
                is Parameter.StringParam -> (params[param.name] as? String)?.let { stringValue ->
                    when {
                        param.optional.not() && stringValue.isEmpty() -> false
                        param.multiLine.not() && stringValue.lines().size > 1 -> false
                        else -> true
                    }
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
        val label = json.decodeFromJsonElement<LocalizedJsonString>(requireNotNull(element["label"]))
        val description = element["description"]?.let { json.decodeFromJsonElement<LocalizedJsonString>(it) }
        val enableIf = element["enableIf"]?.jsonPrimitive?.content
        val defaultValue = requireNotNull(element["defaultValue"])
        return when (type) {
            Plugin.ParameterType.Integer -> {
                val default = defaultValue.jsonPrimitive.int
                val min = element["min"]?.jsonPrimitive?.int
                val max = element["max"]?.jsonPrimitive?.int
                Plugin.Parameter.IntParam(name, label, description, enableIf, default, min, max)
            }
            Plugin.ParameterType.Float -> {
                val default = defaultValue.jsonPrimitive.float
                val min = element["min"]?.jsonPrimitive?.float
                val max = element["max"]?.jsonPrimitive?.float
                Plugin.Parameter.FloatParam(name, label, description, enableIf, default, min, max)
            }
            Plugin.ParameterType.Boolean -> {
                val default = defaultValue.jsonPrimitive.boolean
                Plugin.Parameter.BooleanParam(name, label, description, enableIf, default)
            }
            Plugin.ParameterType.String -> {
                val defaultValueFromFile = element["defaultValueFromFile"]?.jsonPrimitive?.content
                val default = defaultValueFromFile
                    ?.let(Plugin.Parameter.StringParam::getDefaultValueFromFile)
                    ?: defaultValue.jsonPrimitive.content
                val optional = requireNotNull(element["optional"]).jsonPrimitive.boolean
                val multiLine = element["multiLine"]?.jsonPrimitive?.boolean ?: false
                Plugin.Parameter.StringParam(name, label, description, enableIf, default, multiLine, optional)
            }
            Plugin.ParameterType.Enum -> {
                val default = json.decodeFromJsonElement<LocalizedJsonString>(defaultValue)
                val options = requireNotNull(element["options"]).jsonArray.map {
                    json.decodeFromJsonElement<LocalizedJsonString>(it)
                }
                Plugin.Parameter.EnumParam(name, label, description, enableIf, default, options)
            }
            Plugin.ParameterType.EntrySelector -> {
                val default = decoder.json.decodeFromJsonElement(EntrySelector.serializer(), defaultValue)
                Plugin.Parameter.EntrySelectorParam(name, label, description, enableIf, default)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Plugin.Parameter<*>) {
        // nop
    }
}
