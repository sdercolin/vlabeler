package com.sdercolin.vlabeler.tracking

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.tracking.event.CreateProjectEvent
import com.sdercolin.vlabeler.tracking.event.MacroPluginUsageEvent
import com.sdercolin.vlabeler.tracking.event.SaveAppConfEvent
import com.sdercolin.vlabeler.tracking.event.TemplatePluginUsageEvent
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.jsonMinified
import com.sdercolin.vlabeler.util.orEmpty
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.serialization.encodeToString

fun AppState.trackProjectCreation(project: Project, byIpcRequest: Boolean = false, byQuickEdit: Boolean = false) {
    val createProjectEvent = CreateProjectEvent(
        labelerName = project.labelerConf.name,
        labelerNameVer = "${project.labelerConf.name} ${project.labelerConf.version}",
        params = project.labelerParams.orEmpty().stripFilePaths().stringifyJson(),
        autoExport = project.autoExport,
        byIpcRequest = byIpcRequest,
        byQuickEdit = byQuickEdit,
    )
    track(createProjectEvent)
}

fun AppState.trackTemplateGeneration(plugin: Plugin, params: ParamMap?) {
    val templatePluginUsageEvent = TemplatePluginUsageEvent(
        pluginName = plugin.name,
        pluginNameVer = "${plugin.name} ${plugin.version}",
        params = ParamTypedMap.from(params.orEmpty(), plugin.parameterDefs).orEmpty().stripFilePaths().stringifyJson(),
    )
    track(templatePluginUsageEvent)
}

fun AppState.trackMacroPluginExecution(plugin: Plugin, params: ParamMap?, quickLaunch: Boolean) {
    val macroPluginUsageEvent = MacroPluginUsageEvent(
        pluginName = plugin.name,
        pluginNameVer = "${plugin.name} ${plugin.version}",
        params = ParamTypedMap.from(params.orEmpty(), plugin.parameterDefs).orEmpty().stripFilePaths().stringifyJson(),
        quickLaunch = quickLaunch,
    )
    track(macroPluginUsageEvent)
}

fun AppState.trackNewAppConf(appConf: AppConf) {
    track(SaveAppConfEvent(jsonMinified.encodeToString(appConf)))
}
