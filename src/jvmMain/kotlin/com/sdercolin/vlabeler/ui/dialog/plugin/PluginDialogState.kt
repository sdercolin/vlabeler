package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.runtime.mutableStateListOf
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toParamMap
import com.sdercolin.vlabeler.util.toUri
import java.awt.Desktop

class PluginDialogState(
    val plugin: Plugin,
    paramMap: ParamMap,
    private val savedParamMap: ParamMap?,
    val project: Project?,
    private val submit: (ParamMap?) -> Unit,
    private val save: (ParamMap) -> Unit,
) {
    val paramDefs = plugin.parameters?.list.orEmpty()
    val params = mutableStateListOf(*paramMap.map { it.value }.toTypedArray())
    private val parseErrors = mutableStateListOf(*paramMap.map { false }.toTypedArray())
    val hasParams get() = paramDefs.isNotEmpty()

    private fun getCurrentParamMap() = params.mapIndexed { index, value ->
        paramDefs[index].name to value
    }.toMap().toParamMap()

    fun apply() {
        submit(getCurrentParamMap())
    }

    fun cancel() {
        submit(null)
    }

    fun getLabel(index: Int): String {
        return paramDefs[index].label
    }

    fun getDescription(index: Int): String {
        val description = paramDefs[index].description
        val range: Pair<String?, String?> = when (val def = paramDefs[index]) {
            is Plugin.Parameter.FloatParam -> def.min?.toString() to def.max?.toString()
            is Plugin.Parameter.IntParam -> def.min?.toString() to def.max?.toString()
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

    fun isValid(index: Int): Boolean {
        val value = params[index]
        return when (val def = paramDefs[index]) {
            is Plugin.Parameter.BooleanParam -> true
            is Plugin.Parameter.EnumParam -> true
            is Plugin.Parameter.FloatParam -> {
                val floatValue = value as? Float ?: return false
                floatValue in (def.min ?: Float.NEGATIVE_INFINITY)..(def.max ?: Float.POSITIVE_INFINITY)
            }
            is Plugin.Parameter.IntParam -> {
                val intValue = value as? Int ?: return false
                intValue in (def.min ?: Int.MIN_VALUE)..(def.max ?: Int.MAX_VALUE)
            }
            is Plugin.Parameter.StringParam -> {
                val stringValue = value as? String ?: return false
                val fulfillMultiLine = if (def.multiLine.not()) {
                    stringValue.lines().size < 2
                } else true
                val fulfillOptional = if (def.optional.not()) {
                    stringValue.isNotEmpty()
                } else true
                fulfillMultiLine && fulfillOptional
            }
            is Plugin.Parameter.EntrySelectorParam -> {
                val entrySelectorValue = value as? EntrySelector ?: return false
                val labelerConf = requireNotNull(project?.labelerConf) {
                    "labelerConf is required for a EntrySelectorParam"
                }
                entrySelectorValue.filters.all { it.isValid(labelerConf) }
            }
        }
    }

    fun isAllValid() = params.indices.all { isValid(it) } && parseErrors.none { it }

    fun update(index: Int, value: Any) {
        params[index] = value
    }

    fun setParseError(index: Int, isError: Boolean) {
        parseErrors[index] = isError
    }

    fun canReset() = paramDefs.indices.all {
        params[it] == paramDefs[it].defaultValue
    }

    fun reset() {
        paramDefs.indices.forEach {
            params[it] = paramDefs[it].defaultValue
        }
    }

    fun openEmail() {
        Desktop.getDesktop().browse("mailto:${plugin.email}".toUri())
    }

    fun openWebsite() {
        val uri = plugin.website.takeIf { it.isNotBlank() }?.toUri() ?: return
        Desktop.getDesktop().browse(uri)
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
        is Plugin.Parameter.EntrySelectorParam -> false
        is Plugin.Parameter.StringParam -> param.multiLine.not()
        else -> true
    }
}
