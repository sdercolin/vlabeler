package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.stringifyJson

class LabelerDialogState(
    val labeler: LabelerConf,
    private val isExistingProject: Boolean,
    override val snackbarHostState: SnackbarHostState,
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val submit: (ParamMap?) -> Unit,
    override val save: (ParamMap) -> Unit,
    override val load: (ParamMap) -> Unit,
) : BasePluginDialogState(paramMap) {

    override val project: Project? = null

    override val basePlugin: BasePlugin
        get() = labeler

    override val acceptedParamTypes: List<String> = listOf(
        Parameter.IntParam.Type,
        Parameter.FloatParam.Type,
        Parameter.BooleanParam.Type,
        Parameter.StringParam.Type,
        Parameter.EnumParam.Type,
        Parameter.FileParam.Type,
        Parameter.RawFileParam.Type,
    )

    override fun isChangeable(parameterName: String): Boolean {
        if (isExistingProject.not()) return true
        labeler.parameters.find { it.parameter.name == parameterName }?.let {
            return it.changeable
        }
        throw IllegalArgumentException("Parameter $parameterName not found")
    }

    override suspend fun import(target: BasePluginPresetTarget) = runCatching {
        when (target) {
            is BasePluginPresetTarget.File -> {
                val preset = target.file.readText().parseJson<BasePluginPreset>()
                if (preset.pluginName != labeler.name) {
                    throw IllegalArgumentException("Labeler name mismatch: ${preset.pluginName} != ${labeler.name}")
                }
                load(preset.params.resolve(labeler).retainUnchangeableItems())
            }
            is BasePluginPresetTarget.Memory -> {
                val params = ParamTypedMap.from(
                    basePlugin.loadSavedParams(basePlugin.getSavedParamsFile()),
                    basePlugin.parameterDefs,
                )
                load(params.resolve(labeler).retainUnchangeableItems())
            }
        }
    }
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogImportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogImportFailure))
        }

    override suspend fun export(target: BasePluginPresetTarget) = runCatching {
        val params = getCurrentParamMap()
        when (target) {
            is BasePluginPresetTarget.File -> {
                val preset = BasePluginPreset(
                    pluginName = labeler.name,
                    pluginVersion = labeler.version,
                    params = ParamTypedMap.from(params, labeler.parameterDefs),
                )
                target.file.writeText(preset.stringifyJson())
            }
            is BasePluginPresetTarget.Memory -> {
                labeler.saveParams(params, labeler.getSavedParamsFile())
            }
        }
    }
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogExportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogExportFailure))
        }
}
