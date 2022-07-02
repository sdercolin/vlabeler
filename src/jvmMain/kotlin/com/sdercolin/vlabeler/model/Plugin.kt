@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.Python
import com.sdercolin.vlabeler.util.json
import com.sdercolin.vlabeler.util.parseJson
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
import org.python.core.PyObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

/**
 * Only deserialization is supported
 */
@Serializable
@Immutable
data class Plugin(
    val name: String,
    val version: Int = 1,
    val type: Type,
    val displayedName: String = name,
    val author: String,
    val description: String = "",
    val supportedLabelFileExtension: String,
    val inputFileExtension: String? = null,
    val requireInputFile: Boolean = false,
    val allowMultipleInputFiles: Boolean = false,
    val parameters: Parameters? = null,
    val scriptFiles: List<String>,
    @Transient val directory: File? = null
) {

    fun readScriptTexts() = scriptFiles.map { requireNotNull(directory).resolve(it).readText() }
    fun getDefaultParams() = parameters?.list.orEmpty().associate { parameter ->
        parameter.name to requireNotNull(parameter.defaultValue)
    }

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
    sealed class Parameter<T>(
        val type: ParameterType,
        val name: String,
        val label: String,
        open val defaultValue: T,
    ) {
        class IntParam(
            name: String,
            label: String,
            defaultValue: Int,
            val min: Int?,
            val max: Int?
        ) : Parameter<Int>(ParameterType.Integer, name, label, defaultValue)

        class FloatParam(
            name: String,
            label: String,
            defaultValue: Float,
            val min: Float?,
            val max: Float?
        ) : Parameter<Float>(ParameterType.Float, name, label, defaultValue)

        class BooleanParam(
            name: String,
            label: String,
            defaultValue: Boolean
        ) : Parameter<Boolean>(ParameterType.Boolean, name, label, defaultValue)

        class StringParam(
            name: String,
            label: String,
            defaultValue: String
        ) : Parameter<String>(ParameterType.String, name, label, defaultValue)

        class EnumParam(
            name: String,
            label: String,
            defaultValue: String,
            val options: List<String>
        ) : Parameter<String>(ParameterType.Enum, name, label, defaultValue)
    }

    @Serializable
    enum class ParameterType {
        @SerialName("int")
        Integer,

        @SerialName("float")
        Float,

        @SerialName("bool")
        Boolean,

        @SerialName("string")
        String,

        @SerialName("enum")
        Enum
    }
}

@Serializer(Plugin.Parameters::class)
object PluginParameterListSerializer : KSerializer<Plugin.Parameters> {
    override fun deserialize(decoder: Decoder): Plugin.Parameters {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        val list = requireNotNull(element["list"]).jsonArray.map {
            json.decodeFromJsonElement(PluginParameterSerializer, it)
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
        val type = parseJson<Plugin.ParameterType>(requireNotNull(element["type"]).jsonPrimitive.content)
        val name = requireNotNull(element["name"]).jsonPrimitive.content
        val label = requireNotNull(element["label"]).jsonPrimitive.content
        val defaultPrimitive = requireNotNull(element["defaultValue"]).jsonPrimitive
        return when (type) {
            Plugin.ParameterType.Integer -> {
                val default = defaultPrimitive.int
                val min = element["min"]?.jsonPrimitive?.int
                val max = element["max"]?.jsonPrimitive?.int
                Plugin.Parameter.IntParam(name, label, default, min, max)
            }
            Plugin.ParameterType.Float -> {
                val default = defaultPrimitive.float
                val min = element["min"]?.jsonPrimitive?.float
                val max = element["max"]?.jsonPrimitive?.float
                Plugin.Parameter.FloatParam(name, label, default, min, max)
            }
            Plugin.ParameterType.Boolean -> {
                val default = defaultPrimitive.boolean
                Plugin.Parameter.BooleanParam(name, label, default)
            }
            Plugin.ParameterType.String -> {
                val default = defaultPrimitive.content
                Plugin.Parameter.StringParam(name, label, default)
            }
            Plugin.ParameterType.Enum -> {
                val default = defaultPrimitive.content
                val options = requireNotNull(element["options"]).jsonArray.map {
                    it.jsonPrimitive.content
                }
                Plugin.Parameter.EnumParam(name, label, default, options)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Plugin.Parameter<*>) {
        // nop
    }
}

fun runTemplatePlugin(
    plugin: Plugin,
    params: Map<String, Any>,
    inputFiles: List<File>,
    encoding: String,
    sampleNames: List<String>
): List<FlatEntry> {
    val python = Python()
    val inputTexts = inputFiles.map { it.readText(Charset.forName(encoding)) }
    python.setCurrentWorkingDirectory(requireNotNull(plugin.directory).absolutePath)

    val outputStream = ByteArrayOutputStream()
    python.setOutputStream(outputStream)

    python.set("inputs", inputTexts)
    python.set("samples", sampleNames)
    python.set("params", params)

    val entryDefCode = useResource("template_entry.py") { String(it.readAllBytes()) }
    python.exec(entryDefCode)

    plugin.readScriptTexts().forEach {
        python.exec(it)
    }

    val output = python.get<List<PyObject>>("output")
        .map { obj ->
            FlatEntry(
                sample = obj.__getattr__("sample").asStringOrNull(),
                name = obj.__getattr__("name").asString(),
                start = obj.__getattr__("start").asDouble().toFloat(),
                end = obj.__getattr__("end").asDouble().toFloat(),
                points = obj.__getattr__("points").asIterable().map { it.asDouble().toFloat() },
                extra = obj.__getattr__("extras").asIterable().map { it.asString() }
            )
        }
    Log.info("Plugin execution got entries:\n" + output.joinToString("\n"))
    val printed = outputStream.toByteArray().decodeToString()
    if (printed.isNotBlank()) {
        Log.debug("Plugin execution output:\n$printed")
    }
    outputStream.close()
    python.close()
    return output
}
