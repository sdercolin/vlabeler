@file:OptIn(ExperimentalSerializationApi::class)

package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.RecordDir
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
 * Only deserialization is supported. See [docs/plugin-development.md] for more information.
 */
@Serializable
@Immutable
data class Plugin(
    override val name: String,
    override val version: Int = 1,
    val type: Type,
    override val displayedName: LocalizedJsonString = name.toLocalized(),
    override val author: String,
    override val email: String = "",
    override val description: LocalizedJsonString = "".toLocalized(),
    override val website: String = "",
    val supportedLabelFileExtension: String,
    val outputRawEntry: Boolean = false,
    val parameters: Parameters? = null,
    val scriptFiles: List<String>,
    override val resourceFiles: List<String> = listOf(),
    val inputFinderScriptFile: String? = null,
    val scope: PluginProcessScope = PluginProcessScope.Module,
    @Transient override val directory: File? = null,
    @Transient val builtIn: Boolean = false,
) : BasePlugin {

    enum class PluginProcessScope {
        Project,
        Module
    }

    override val parameterDefs: List<Parameter<*>>
        get() = parameters?.list.orEmpty()

    override val isSelfExecutable: Boolean
        get() = when (type) {
            Type.Template -> false
            Type.Macro -> true
        }

    fun readScriptTexts() = scriptFiles.map { requireNotNull(directory).resolve(it).readText() }

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

    suspend fun saveMacroParams(paramMap: ParamMap, defaultFile: File, appRecordStore: AppRecordStore, slot: Int?) {
        if (slot == null) {
            saveParams(paramMap, defaultFile)
        } else {
            val paramTypedMap = ParamTypedMap.from(paramMap, parameterDefs)
            val quickLaunch = PluginQuickLaunch(
                pluginName = name,
                params = paramTypedMap,
            )
            appRecordStore.update {
                val existing = pluginQuickLaunchSlots[slot]
                saveQuickLaunch(slot, quickLaunch.copy(skipDialog = existing?.skipDialog ?: false))
            }
        }
    }

    override fun getSavedParamsFile(): File = RecordDir.resolve(name + PLUGIN_SAVED_PARAMS_FILE_EXTENSION)

    @Serializer(Parameters::class)
    object PluginParameterListSerializer : KSerializer<Parameters> {
        override fun deserialize(decoder: Decoder): Parameters {
            require(decoder is JsonDecoder)
            val element = decoder.decodeJsonElement()
            require(element is JsonObject)
            val list = requireNotNull(element["list"]).jsonArray.map {
                decoder.json.decodeFromJsonElement(PolymorphicSerializer(Parameter::class), it)
            }
            return Parameters(list)
        }
    }

    companion object {
        private const val PLUGIN_SAVED_PARAMS_FILE_EXTENSION = ".plugin.param.json"
    }
}
