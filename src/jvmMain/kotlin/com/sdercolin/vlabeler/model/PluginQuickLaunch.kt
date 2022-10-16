package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.orEmpty
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class PluginQuickLaunch(
    val pluginName: String,
    val params: ParamTypedMap?,
    val skipDialog: Boolean = false,
) {

    fun getMergedParams(plugin: Plugin): ParamMap {
        val savedParams = params?.toParamMap().orEmpty()
        val mergedParams = plugin.parameterDefs.associate {
            it.name to (savedParams[it.name] ?: it.defaultValue)
        }
        return mergedParams.toParamMap()
    }

    fun checkParamsValid(plugin: Plugin, labelerConf: LabelerConf?): Boolean {
        val savedParams = params?.toParamMap().orEmpty()
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
                appState.executeMacroPlugin(plugin, mergedParams.toParamMap())
                appState.hideProgress()
            }
        } else {
            appState.openMacroPluginDialogFromSlot(plugin, mergedParams.toParamMap(), slot)
        }
    }

    companion object {
        const val SlotCount = 8
    }
}
