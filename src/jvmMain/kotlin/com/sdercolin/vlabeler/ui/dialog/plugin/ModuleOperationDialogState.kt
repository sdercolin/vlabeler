package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.ModuleOperationDescriptor
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.stringifyJson

class ModuleOperationDialogState(
    val descriptor: ModuleOperationDescriptor,
    override val snackbarHostState: SnackbarHostState,
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val project: Project?,
    override val submit: (ParamMap?) -> Unit,
    override val save: (ParamMap) -> Unit,
    override val load: (ParamMap) -> Unit,
) : BasePluginDialogState(paramMap) {

    override val basePlugin: BasePlugin
        get() = descriptor

    override val acceptedParamTypes: List<String> = listOf(
        Parameter.IntParam.Type,
        Parameter.FloatParam.Type,
        Parameter.BooleanParam.Type,
        Parameter.StringParam.Type,
        Parameter.EnumParam.Type,
        Parameter.FileParam.Type,
        Parameter.RawFileParam.Type,
    )

    override suspend fun import(target: BasePluginPresetTarget) = runCatching {
        when (target) {
            is BasePluginPresetTarget.File -> {
                val preset = target.file.readText().parseJson<BasePluginPreset>()
                if (preset.pluginName != descriptor.name) {
                    throw IllegalArgumentException(
                        "Module operation name mismatch: ${preset.pluginName} != ${descriptor.name}",
                    )
                }
                load(preset.params.resolve(descriptor).retainUnchangeableItems())
            }
            is BasePluginPresetTarget.Memory -> {
                val params = ParamTypedMap.from(
                    basePlugin.loadSavedParams(basePlugin.getSavedParamsFile()),
                    basePlugin.parameterDefs,
                )
                load(params.resolve(descriptor).retainUnchangeableItems())
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
                    pluginName = descriptor.name,
                    pluginVersion = descriptor.version,
                    params = ParamTypedMap.from(params, descriptor.parameterDefs),
                )
                target.file.writeText(preset.stringifyJson())
            }
            is BasePluginPresetTarget.Memory -> {
                descriptor.saveParams(params, descriptor.getSavedParamsFile())
            }
        }
    }
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogExportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogExportFailure))
        }
}
