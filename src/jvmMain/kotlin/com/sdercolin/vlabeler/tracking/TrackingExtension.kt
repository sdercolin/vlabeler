package com.sdercolin.vlabeler.tracking

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.BasePlugin
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.tracking.event.BasePluginBox
import com.sdercolin.vlabeler.tracking.event.CreateProjectEvent
import com.sdercolin.vlabeler.tracking.event.MacroPluginUsageEvent
import com.sdercolin.vlabeler.tracking.event.SaveAppConfEvent
import com.sdercolin.vlabeler.tracking.event.TemplatePluginUsageEvent
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.jsonMinified
import com.sdercolin.vlabeler.util.orEmpty
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

fun AppState.trackProjectCreation(project: Project, byIpcRequest: Boolean) {
    val labelerUsageEvent = CreateProjectEvent(
        labeler = project.labelerConf.toBox(),
        params = project.labelerParams.orEmpty().stripFilePaths(),
        autoExport = project.autoExport,
        byIpcRequest = byIpcRequest,
    )
    track(labelerUsageEvent)
}

fun AppState.trackTemplateGeneration(plugin: Plugin, params: ParamMap?) {
    val templatePluginUsageEvent = TemplatePluginUsageEvent(
        plugin = plugin.toBox(),
        params = ParamTypedMap.from(params.orEmpty(), plugin.parameterDefs).orEmpty().stripFilePaths(),
    )
    track(templatePluginUsageEvent)
}

fun AppState.trackMacroPluginExecution(plugin: Plugin, params: ParamMap?, quickLaunch: Boolean) {
    val macroPluginUsageEvent = MacroPluginUsageEvent(
        plugin = plugin.toBox(),
        params = ParamTypedMap.from(params.orEmpty(), plugin.parameterDefs).orEmpty().stripFilePaths(),
        quickLaunch = quickLaunch,
    )
    track(macroPluginUsageEvent)
}

fun AppState.trackNewAppConf(appConf: AppConf) {
    val jsonObject = jsonMinified.encodeToJsonElement(appConf) as JsonObject
    track(SaveAppConfEvent(jsonObject))
}

private fun BasePlugin.toBox() = BasePluginBox(name, version)
