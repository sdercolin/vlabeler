package com.sdercolin.vlabeler.ui.dialog.plugin

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.getSavedParamsFile
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.BasePlugin
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
    paramMap: ParamMap,
    override val savedParamMap: ParamMap?,
    override val project: Project?,
    override val submit: (ParamMap?) -> Unit,
    override val load: (ParamMap) -> Unit,
    override val save: (ParamMap) -> Unit,
    override val showSnackbar: suspend (String) -> Unit,
    val executable: Boolean,
    val slot: Int? = null,
) : BasePluginDialogState(paramMap) {

    override val basePlugin: BasePlugin
        get() = plugin

    private fun getPresetItems(
        record: AppRecord,
        isAvailable: (BasePluginPresetItem.Memory) -> Boolean,
    ): List<BasePluginPresetItem> {
        if (plugin.type == Plugin.Type.Template) return getDefaultPresetTargets()

        val root = getDefaultPresetTargets()
            .filterIsInstance<BasePluginPresetItem.Memory>()
            .first()
            .copy(isCurrent = slot == null)

        val slots = List(PluginQuickLaunch.SlotCount) { index ->
            val quickLaunch = record.pluginQuickLaunchSlots[index]
            val preset = if (quickLaunch?.pluginName == plugin.name) {
                BasePluginPreset(
                    plugin.name,
                    plugin.version,
                    quickLaunch.params,
                )
            } else {
                null
            }
            BasePluginPresetItem.Memory(
                preset = preset,
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
        isAvailable = { it.isCurrent || it.preset?.pluginName == plugin.name },
    )

    override fun getExportablePresets(record: AppRecord): List<BasePluginPresetItem> = getPresetItems(
        record = record,
        isAvailable = { it.isCurrent || it.preset?.pluginName == plugin.name || it.preset == null },
    )

    override suspend fun import(target: BasePluginPresetTarget) = runCatching {
        when (target) {
            is BasePluginPresetTarget.File -> {
                val preset = target.file.readText().parseJson<BasePluginPreset>()
                if (preset.pluginName != plugin.name) {
                    throw IllegalArgumentException("Plugin name mismatch: ${preset.pluginName} != ${plugin.name}")
                }
                load(preset.params.resolve(plugin))
            }
            is BasePluginPresetTarget.Memory -> load(requireNotNull(target.item.preset).params.resolve(plugin))
        }
    }
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogImportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogImportFailure))
        }

    override suspend fun export(params: ParamMap, target: BasePluginPresetTarget) = runCatching {
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
        .onSuccess { showSnackbar(stringStatic(Strings.PluginDialogImportSuccess)) }
        .getOrElse {
            Log.error(it)
            showSnackbar(stringStatic(Strings.PluginDialogImportFailure))
        }
}
