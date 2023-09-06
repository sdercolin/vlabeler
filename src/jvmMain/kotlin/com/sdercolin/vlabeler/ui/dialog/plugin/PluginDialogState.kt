package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.PluginQuickLaunch
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.stringifyJson

class PluginDialogState(
    val plugin: Plugin,
    private val appRecordStore: AppRecordStore,
    override val snackbarHostState: SnackbarHostState,
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val project: Project?,
    override val submit: (ParamMap?) -> Unit,
    override val load: (ParamMap) -> Unit,
    override val save: (ParamMap) -> Unit,
    val executable: Boolean,
    val slot: Int? = null,
) : BasePluginDialogState(paramMap) {

    override val basePlugin: BasePlugin
        get() = plugin

    override val acceptedParamTypes: List<String> = listOfNotNull(
        Parameter.IntParam.Type,
        Parameter.FloatParam.Type,
        Parameter.BooleanParam.Type,
        Parameter.StringParam.Type,
        Parameter.EnumParam.Type,
        if (plugin.type == Plugin.Type.Macro) Parameter.EntrySelectorParam.Type else null,
        Parameter.FileParam.Type,
        Parameter.RawFileParam.Type,
    )

    private fun getPresetItems(
        record: AppRecord,
        isAvailable: (BasePluginPresetItem.Memory) -> Boolean,
    ): List<BasePluginPresetItem> {
        if (plugin.type == Plugin.Type.Template) return getDefaultPresetTargets()

        val root = getDefaultPresetTargets()
            .filterIsInstance<BasePluginPresetItem.Memory>()
            .first()
            .copy(isCurrent = slot == null)

        val slots = List(PluginQuickLaunch.SLOT_COUNT) { index ->
            val quickLaunch = record.pluginQuickLaunchSlots[index]
            BasePluginPresetItem.Memory(
                pluginName = quickLaunch?.pluginName,
                slot = index,
                isCurrent = slot == index,
                available = false,
            ).run {
                if (isAvailable(this)) copy(available = true) else this
            }
        }

        return listOf(root) + slots + listOf(BasePluginPresetItem.File)
    }

    override fun getImportablePresets(record: AppRecord) = getPresetItems(
        record = record,
        isAvailable = { it.isCurrent || it.pluginName == plugin.name },
    )

    override fun getExportablePresets(record: AppRecord): List<BasePluginPresetItem> = getPresetItems(
        record = record,
        isAvailable = { it.isCurrent || it.pluginName == plugin.name || it.pluginName == null },
    )

    override suspend fun import(target: BasePluginPresetTarget) = runCatching {
        when (target) {
            is BasePluginPresetTarget.File -> {
                val preset = target.file.readText().parseJson<BasePluginPreset>()
                if (preset.pluginName != plugin.name) {
                    throw IllegalArgumentException("Plugin name mismatch: ${preset.pluginName} != ${plugin.name}")
                }
                load(preset.params.resolve(plugin).retainUnchangeableItems())
            }
            is BasePluginPresetTarget.Memory -> {
                val slot = target.item.slot
                if (slot == null) {
                    val params = ParamTypedMap.from(
                        basePlugin.loadSavedParams(basePlugin.getSavedParamsFile()),
                        basePlugin.parameterDefs,
                    )
                    load(params.resolve(plugin).retainUnchangeableItems())
                } else {
                    val record = appRecordStore.value
                    val quickLaunch = record.pluginQuickLaunchSlots[slot]
                    if (quickLaunch?.pluginName != plugin.name) {
                        throw IllegalArgumentException(
                            "Plugin name mismatch: ${quickLaunch?.pluginName} != ${plugin.name}",
                        )
                    }
                    load(quickLaunch.params.resolve(plugin).retainUnchangeableItems())
                }
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
                    pluginName = plugin.name,
                    pluginVersion = plugin.version,
                    params = ParamTypedMap.from(params, plugin.parameterDefs),
                )
                target.file.writeText(preset.stringifyJson())
            }
            is BasePluginPresetTarget.Memory -> {
                val targetSlot = target.item.slot
                when (plugin.type) {
                    Plugin.Type.Macro -> {
                        plugin.saveMacroParams(params, plugin.getSavedParamsFile(), appRecordStore, targetSlot)
                    }
                    Plugin.Type.Template -> {
                        plugin.saveParams(params, plugin.getSavedParamsFile())
                    }
                }
            }
        }
    }
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogExportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogExportFailure))
        }
}
