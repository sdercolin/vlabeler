@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toFileOrNull
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

    @Serializable(with = PluginParameterListSerializer::class)
    class Parameters(
        val list: List<Parameter<*>>,
    )

    @Serializable(with = PluginParameterSerializer::class)
    @Immutable
    sealed class Parameter<T : Any> {
        abstract val name: String
        abstract val label: LocalizedJsonString
        abstract val description: LocalizedJsonString?
        abstract val enableIf: String?
        abstract val defaultValue: T
        abstract fun eval(value: Any): Boolean

        @Serializable
        @SerialName("integer")
        class IntParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: Int,
            val min: Int? = null,
            val max: Int? = null,
        ) : Parameter<Int>() {

            override fun eval(value: Any) = value is Int && value != 0
        }

        @Serializable
        @SerialName("float")
        class FloatParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: Float,
            val min: Float? = null,
            val max: Float? = null,
        ) : Parameter<Float>() {

            override fun eval(value: Any) = value is Float && value != 0f && value.isNaN().not()
        }

        @Serializable
        @SerialName("boolean")
        class BooleanParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: Boolean,
        ) : Parameter<Boolean>() {

            override fun eval(value: Any) = value is Boolean && value
        }

        @Serializable
        @SerialName("string")
        class StringParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: String,
            val multiLine: Boolean = false,
            val optional: Boolean = false,
        ) : Parameter<String>() {

            override fun eval(value: Any) = value is String && value.isNotEmpty()

            companion object {
                val DefaultValueFileReferencePattern = Regex("^file::(.*)$")
                fun getDefaultValueFromFile(fileName: String) = "file::$fileName"
            }
        }

        @Serializable
        @SerialName("enum")
        class EnumParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: LocalizedJsonString,
            val options: List<LocalizedJsonString>,
        ) : Parameter<LocalizedJsonString>() {

            override fun eval(value: Any) = value is LocalizedJsonString
        }

        @Serializable
        @SerialName("entrySelector")
        class EntrySelectorParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: EntrySelector,
        ) : Parameter<EntrySelector>() {

            override fun eval(value: Any) = value is EntrySelector && value.filters.isNotEmpty()
        }

        @Serializable
        @SerialName("file")
        class FileParam(
            override val name: String,
            override val label: LocalizedJsonString,
            override val description: LocalizedJsonString? = null,
            override val enableIf: String? = null,
            override val defaultValue: FileWithEncoding,
            val optional: Boolean = false,
            val acceptExtensions: List<String>? = null,
        ) : Parameter<FileWithEncoding>() {

            override fun eval(value: Any) = value is FileWithEncoding && value.file != null
        }
    }

    fun checkParam(param: Parameter<*>, value: Any, labelerConf: LabelerConf?): Boolean {
        return when (param) {
            is Parameter.BooleanParam -> (value as? Boolean) != null
            is Parameter.EnumParam -> (value as? LocalizedJsonString)?.let { enumValue ->
                enumValue in param.options
            } == true
            is Parameter.FloatParam -> (value as? Float)?.let { floatValue ->
                floatValue >= (param.min ?: Float.NEGATIVE_INFINITY) &&
                    floatValue <= (param.max ?: Float.POSITIVE_INFINITY)
            } == true
            is Parameter.IntParam -> (value as? Int)?.let { intValue ->
                intValue >= (param.min ?: Int.MIN_VALUE) && intValue <= (param.max ?: Int.MAX_VALUE)
            } == true
            is Parameter.StringParam -> (value as? String)?.let { stringValue ->
                when {
                    param.optional.not() && stringValue.isEmpty() -> false
                    param.multiLine.not() && stringValue.lines().size > 1 -> false
                    else -> true
                }
            } == true
            is Parameter.EntrySelectorParam -> (value as? EntrySelector)?.let { selector ->
                selector.filters.all { it.isValid(requireNotNull(labelerConf)) }
            } == true
            is Parameter.FileParam -> (value as? FileWithEncoding)?.let {
                if (param.optional && it.file == null) return true
                val file = it.file?.toFileOrNull(ensureIsFile = true) ?: return@let false
                if (param.acceptExtensions != null && file.extension !in param.acceptExtensions) return@let false
                true
            } == true
        }
    }

    fun checkParams(params: ParamMap, labelerConf: LabelerConf?): Boolean =
        parameters?.list.orEmpty().all { param ->
            val value = params[param.name] ?: return@all false
            checkParam(param, value, labelerConf)
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

object PluginParameterSerializer : JsonContentPolymorphicSerializer<Plugin.Parameter<*>>(Plugin.Parameter::class) {

    override fun selectDeserializer(element: JsonElement) = when (element.jsonObject["type"]?.jsonPrimitive?.content) {
        "integer" -> Plugin.Parameter.IntParam.serializer()
        "float" -> Plugin.Parameter.FloatParam.serializer()
        "boolean" -> Plugin.Parameter.BooleanParam.serializer()
        "string" -> Plugin.Parameter.StringParam.serializer()
        "enum" -> Plugin.Parameter.EnumParam.serializer()
        "entrySelector" -> Plugin.Parameter.EntrySelectorParam.serializer()
        "file" -> Plugin.Parameter.FileParam.serializer()
        else -> throw IllegalArgumentException("Unknown parameter type")
    }
}
