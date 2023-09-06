package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Url
import com.sdercolin.vlabeler.util.toParamMap

abstract class BasePluginDialogState(paramMap: ParamMap) {
    abstract val basePlugin: BasePlugin
    protected abstract val savedParamMap: ParamMap?
    abstract val project: Project?
    protected abstract val submit: (ParamMap?) -> Unit
    protected abstract val load: (ParamMap) -> Unit
    protected abstract val save: (ParamMap) -> Unit
    abstract suspend fun import(target: BasePluginPresetTarget)
    abstract suspend fun export(target: BasePluginPresetTarget)

    open fun isChangeable(parameterName: String): Boolean = true

    abstract val snackbarHostState: SnackbarHostState
    suspend fun showSnackbar(message: String) {
        snackbarHostState.showSnackbar(message, actionLabel = stringStatic(Strings.CommonOkay))
    }

    val paramDefs: List<Parameter<*>> get() = basePlugin.parameterDefs
    val params = mutableStateListOf(*paramMap.map { it.value }.toTypedArray())
    private val parseErrors = mutableStateListOf(*paramMap.map { false }.toTypedArray())
    val hasParams get() = paramDefs.isNotEmpty()
    val needJsClient get() = paramDefs.any { it is Parameter.EntrySelectorParam }

    abstract val acceptedParamTypes: List<String>

    fun acceptParamType(type: String) = acceptedParamTypes.contains(type)

    fun getCurrentParamMap() = params.mapIndexed { index, value ->
        paramDefs[index].name to value
    }.toMap().toParamMap()

    fun apply() {
        submit(getCurrentParamMap())
    }

    fun cancel() {
        submit(null)
    }

    @Composable
    fun getLabel(index: Int): String {
        return paramDefs[index].label.get()
    }

    @Composable
    fun getDescription(index: Int): String {
        val description = paramDefs[index].description?.get()
        val range: Pair<String?, String?> = when (val def = paramDefs[index]) {
            is Parameter.FloatParam -> def.min?.toString() to def.max?.toString()
            is Parameter.IntParam -> def.min?.toString() to def.max?.toString()
            else -> null to null
        }
        val suffix = when {
            range.first != null && range.second != null ->
                string(Strings.PluginDialogDescriptionMinMax, range.first, range.second)
            range.first != null ->
                string(Strings.PluginDialogDescriptionMin, range.first)
            range.second != null ->
                string(Strings.PluginDialogDescriptionMax, range.second)
            else -> null
        }
        return listOfNotNull(description, suffix).joinToString("\n")
    }

    fun isValid(index: Int): Boolean = paramDefs[index].check(params[index], project?.labelerConf)

    fun isAllValid() = params.indices.all { isValid(it) } && parseErrors.none { it }

    fun update(index: Int, value: Any) {
        params[index] = value
    }

    fun setParseError(index: Int, isError: Boolean) {
        parseErrors[index] = isError
    }

    private fun getResetParamMap() =
        paramDefs.associate { it.name to it.defaultValue }.toParamMap().retainUnchangeableItems()

    fun canReset(): Boolean {
        val reset = getResetParamMap()
        val current = getCurrentParamMap()
        return reset != current
    }

    fun reset() {
        getResetParamMap().map { it.value }.forEachIndexed { index, value ->
            params[index] = value
        }
    }

    fun openEmail() {
        Url.open("mailto:${basePlugin.email}")
    }

    fun openWebsite() {
        val url = basePlugin.website.takeIf { it.isNotBlank() } ?: return
        Url.open(url)
    }

    fun canSave(): Boolean {
        val current = runCatching { getCurrentParamMap().toList() }.getOrNull() ?: return false
        val saved = savedParamMap?.toList() ?: return false
        val changed = saved.indices.all { saved[it] == current[it] }.not()
        return changed && isAllValid()
    }

    fun save() {
        save(getCurrentParamMap())
    }

    fun isParamInRow(index: Int): Boolean = when (val param = paramDefs[index]) {
        is Parameter.EntrySelectorParam -> false
        is Parameter.FileParam -> false
        is Parameter.RawFileParam -> false
        is Parameter.StringParam -> param.multiLine.not()
        else -> true
    }

    protected fun getDefaultPresetTargets(): List<BasePluginPresetItem> = listOf(
        BasePluginPresetItem.Memory(
            pluginName = basePlugin.name,
            slot = null,
            isCurrent = true,
            available = true,
        ),
        BasePluginPresetItem.File,
    )

    open fun getImportablePresets(record: AppRecord): List<BasePluginPresetItem> = getDefaultPresetTargets()

    open fun getExportablePresets(record: AppRecord): List<BasePluginPresetItem> = getDefaultPresetTargets()

    fun ParamMap.retainUnchangeableItems(): ParamMap {
        val result = this.toMutableMap()
        paramDefs.forEachIndexed { index, parameter ->
            if (isChangeable(parameter.name).not()) {
                result[parameter.name] = params[index]
            }
        }
        return result.toParamMap()
    }
}
