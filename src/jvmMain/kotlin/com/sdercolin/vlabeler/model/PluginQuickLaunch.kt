package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * A data class representing a quick launch command of a plugin.
 *
 * @property pluginName The name of the plugin.
 * @property params The parameters of the plugin.
 * @property skipDialog Whether to skip the dialog and execute the plugin directly.
 */
@Serializable
@Immutable
data class PluginQuickLaunch(
    val pluginName: String,
    val params: ParamTypedMap?,
    val skipDialog: Boolean = false,
) {

    fun getMergedParams(plugin: Plugin): ParamMap = params.resolve(plugin)

    private fun checkParamsValid(plugin: Plugin, labelerConf: LabelerConf?): Boolean {
        val savedParams = params.resolve(plugin)
        return plugin.parameterDefs.all { def ->
            def.check(savedParams[def.name] ?: def.defaultValue, labelerConf)
        }
    }

    fun launch(plugin: Plugin, appState: AppState, slot: Int) {
        val mergedParams = getMergedParams(plugin)
        val allValid = checkParamsValid(plugin, appState.project?.labelerConf)

        if (skipDialog && allValid) {
            appState.mainScope.launch(Dispatchers.IO) {
                appState.showProgress()
                appState.executeMacroPlugin(plugin, mergedParams, slot)
                appState.hideProgress()
            }
        } else {
            appState.openMacroPluginDialogFromSlot(plugin, mergedParams, slot)
        }
    }

    companion object {
        const val SLOT_COUNT = 8
    }
}
