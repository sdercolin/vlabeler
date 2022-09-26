@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
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

    fun validate(): Plugin {
        val allParamTypes = Parameter::class.sealedSubclasses
        val acceptedParamTypes = when (type) {
            Type.Template -> allParamTypes.minus(Parameter.EntrySelectorParam::class)
            Type.Macro -> allParamTypes
        }
        for (parameter in parameters?.list.orEmpty()) {
            require(parameter::class in acceptedParamTypes) {
                "Parameter type ${parameter::class.simpleName} is not supported in plugin type $type"
            }
        }
        return this
    }

    fun checkParams(params: ParamMap, labelerConf: LabelerConf?): Boolean =
        parameters?.list.orEmpty().all { param ->
            val value = params[param.name] ?: return@all false
            param.check(value, labelerConf)
        }
}

@Serializer(Plugin.Parameters::class)
object PluginParameterListSerializer : KSerializer<Plugin.Parameters> {
    override fun deserialize(decoder: Decoder): Plugin.Parameters {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        val list = requireNotNull(element["list"]).jsonArray.map {
            decoder.json.decodeFromJsonElement(PolymorphicSerializer(Parameter::class), it)
        }
        return Plugin.Parameters(list)
    }
}
