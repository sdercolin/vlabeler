package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.exception.PluginUnexpectedRuntimeException
import com.sdercolin.vlabeler.io.getResolvedParamsWithDefaults
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.Serializable

fun runMacroPlugin(
    plugin: Plugin,
    params: ParamMap,
    project: Project,
): Pair<Project, LocalizedJsonString?> {
    val js = JavaScript(
        logHandler = Log.infoFileHandler,
        currentWorkingDirectory = requireNotNull(plugin.directory).absolutePath.toFile(),
    )
    val result = runCatching {
        val resourceTexts = plugin.readResourceFiles()

        js.set("debug", isDebug)
        js.setJson("labeler", project.labelerConf)
        val labelerParams = project.labelerConf.getResolvedParamsWithDefaults(project.labelerParams?.toParamMap(), js)
        js.setJson("labelerParams", labelerParams)
        js.setJson("params", params.resolve(project, js))
        js.setJson("resources", resourceTexts)

        listOfNotNull(
            Resources.classEntryJs,
            Resources.classEditedEntryJs,
            if (plugin.scope == Plugin.PluginProcessScope.Project) Resources.classModuleJs else null,
            Resources.expectedErrorJs,
            Resources.reportJs,
            Resources.fileJs,
        ).forEach { js.execResource(it) }

        when (plugin.scope) {
            Plugin.PluginProcessScope.Project -> {
                js.setJson("modules", project.modules)
                js.set("currentModuleIndex", project.currentModuleIndex)
            }
            Plugin.PluginProcessScope.Module -> {
                js.setJson("entries", project.currentModule.entries)
                js.set("currentEntryIndex", project.currentModule.currentIndex)
            }
        }

        plugin.scriptFiles.zip(plugin.readScriptTexts()).forEach { (file, source) ->
            Log.debug("Launch script: $file")
            js.exec(file, source)
            Log.debug("Finished script: $file")
        }

        val newProject = when (plugin.scope) {
            Plugin.PluginProcessScope.Project -> {
                val modules = js.getJsonOrNull<List<Module>>("modules")
                if (modules != null) {
                    project.copy(
                        modules = modules,
                        currentModuleIndex = js.getOrNull("currentModuleIndex") ?: project.currentModuleIndex,
                    ).validate()
                } else {
                    project
                }
            }
            Plugin.PluginProcessScope.Module -> {
                val editedEntries = js.getJsonOrNull<List<PluginEditedEntry>>("output") // Legacy
                val entries = editedEntries?.map { it.entry } ?: js.getJsonOrNull("entries")
                if (entries != null) {
                    project.updateCurrentModule {
                        copy(
                            entries = entries,
                            currentIndex = js.getOrNull("currentEntryIndex") ?: currentIndex,
                        )
                    }.validate()
                } else {
                    project
                }
            }
        }
        val report = js.getOrNull<String>("reportText")
        newProject to report?.parseJson<LocalizedJsonString>()
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

@Serializable
@Immutable
data class PluginEditedEntry(
    val originalIndex: Int?,
    val entry: Entry,
)
