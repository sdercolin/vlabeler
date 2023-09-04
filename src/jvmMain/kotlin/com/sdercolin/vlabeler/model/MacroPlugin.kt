package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.exception.PluginUnexpectedRuntimeException
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.toFile

class MacroPluginExecutionListener(
    val onAudioPlaybackRequest: (AudioPlaybackRequest) -> Unit,
    val onReport: (LocalizedJsonString) -> Unit,
)

fun runMacroPlugin(
    plugin: Plugin,
    params: ParamMap,
    project: Project,
    listener: MacroPluginExecutionListener,
): Project {
    val js = JavaScript(
        currentWorkingDirectory = requireNotNull(plugin.directory).absolutePath.toFile(),
    )
    val result = runCatching {
        val resourceTexts = plugin.readResourceFiles()

        js.set("debug", isDebug)
        js.setJson("labeler", project.labelerConf)
        val labelerParams = project.labelerParams.resolve(project.labelerConf).resolve(project, js)
        js.setJson("labelerParams", labelerParams)
        js.setJson("params", params.resolve(project, js))
        js.setJson("resources", resourceTexts)

        listOfNotNull(
            Resources.classEntryJs,
            Resources.classModuleJs,
            Resources.expectedErrorJs,
            Resources.reportJs,
            Resources.envJs,
            Resources.fileJs,
            Resources.commandLineJs,
            Resources.requestAudioPlaybackJs,
        ).forEach { js.execResource(it) }

        js.set("pluginDirectory", requireNotNull(plugin.directory))
        js.eval("pluginDirectory = new File(pluginDirectory)")

        when (plugin.scope) {
            Plugin.PluginProcessScope.Project -> {
                js.setJson("modules", project.modules.map { it.toJs(project) })
                js.set("projectRootDirectory", project.rootSampleDirectory)
                js.eval("projectRootDirectory = new File(projectRootDirectory)")
                js.set("currentModuleIndex", project.currentModuleIndex)
            }
            Plugin.PluginProcessScope.Module -> {
                js.setJson("entries", project.currentModule.entries)
                js.set("currentEntryIndex", project.currentModule.currentIndex)
                js.setJson("module", project.currentModule.toJs(project))
            }
        }

        plugin.scriptFiles.zip(plugin.readScriptTexts()).forEach { (file, source) ->
            Log.debug("Launch script: $file")
            js.exec(file, source)
            Log.debug("Finished script: $file")
        }

        val newProject = when (plugin.scope) {
            Plugin.PluginProcessScope.Project -> {
                val modules = js.getJsonOrNull<List<JsModule>>("modules")
                    ?.map { it.toModule(project.rootSampleDirectory) }
                if (modules != null) {
                    project.copy(
                        modules = modules,
                        currentModuleIndex = js.getOrNull("currentModuleIndex") ?: project.currentModuleIndex,
                    ).validate().makeRelativePathsIfPossible()
                } else {
                    project
                }
            }
            Plugin.PluginProcessScope.Module -> {
                val entries = js.getJsonOrNull<List<Entry>>("entries")
                if (entries != null) {
                    project.updateCurrentModule {
                        copy(
                            entries = entries,
                            currentIndex = js.getOrNull("currentEntryIndex") ?: currentIndex,
                        )
                    }.validate().makeRelativePathsIfPossible()
                } else {
                    project
                }
            }
        }
        val report = js.getOrNull<String>("reportText")?.parseJson<LocalizedJsonString>()
        if (report != null) {
            listener.onReport(report)
        }
        val audioPlaybackRequest = js.getJsonOrNull<AudioPlaybackRequest>("audioPlaybackRequest")
        if (audioPlaybackRequest != null) {
            listener.onAudioPlaybackRequest(audioPlaybackRequest)
        }
        newProject
    }.getOrElse {
        val expected = js.getOrNull("expectedError") ?: false
        js.close()
        if (expected) {
            throw PluginRuntimeException(it, it.message?.parseJson())
        } else {
            throw PluginUnexpectedRuntimeException(it)
        }
    }
    js.close()
    return result
}
