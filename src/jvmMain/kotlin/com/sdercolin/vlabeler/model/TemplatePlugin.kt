package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.exception.PluginUnexpectedRuntimeException
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.readTextByEncoding
import com.sdercolin.vlabeler.util.toFile
import java.io.File

/**
 * Data classes to receive the result from a template plugin execution.
 */
sealed class TemplatePluginResult {

    /**
     * Result as raw label lines. It will be then parsed by [LabelerConf.parser].
     */
    data class Raw(val lines: List<String>) : TemplatePluginResult()

    /**
     * Result as parsed [Entry]s.
     */
    data class Parsed(val entries: List<Entry>) : TemplatePluginResult()
}

fun runTemplatePlugin(
    plugin: Plugin,
    params: ParamMap,
    encoding: String,
    sampleFiles: List<File>,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    rootSampleDirectory: String,
    moduleDefinition: ModuleDefinition,
): TemplatePluginResult {
    val js = JavaScript(
        currentWorkingDirectory = requireNotNull(plugin.directory).absolutePath.toFile(),
    )
    val result = runCatching {
        js.set("debug", isDebug)
        js.setJson("labeler", labelerConf)
        js.setJson("labelerParams", labelerParams.resolve(project = null, js = js))
        js.setJson("params", params.resolve(project = null, js = null))

        listOfNotNull(
            if (plugin.outputRawEntry.not()) Resources.classEntryJs else null,
            Resources.expectedErrorJs,
            Resources.envJs,
            Resources.fileJs,
            Resources.commandLineJs,
        ).forEach { js.execResource(it) }

        val inputFinderScriptFile = plugin.inputFinderScriptFile
        val inputTexts = if (inputFinderScriptFile != null) {
            val inputFinderScriptTexts = plugin.directory.resolve(inputFinderScriptFile).readText()
            js.set("root", rootSampleDirectory)
            js.set("moduleName", moduleDefinition.name)
            js.eval("root = new File(root)")
            js.exec(inputFinderScriptFile, inputFinderScriptTexts)
            val encodingByScript = js.getOrNull<String>("encoding")
            js.getJson<List<String>>("inputFilePaths").map {
                it.toFile().readTextByEncoding(encodingByScript ?: encoding)
            }
        } else {
            moduleDefinition.inputFiles?.map {
                runCatching { it.readTextByEncoding(encoding) }.getOrNull()
            } ?: emptyList()
        }
        js.setJson("inputs", inputTexts)
        val resourceTexts = plugin.readResourceFiles()
        js.setJson("resources", resourceTexts)
        js.setJson("samples", sampleFiles.map { it.name })
        js.set("pluginDirectory", requireNotNull(plugin.directory))
        js.eval("pluginDirectory = new File(pluginDirectory)")

        plugin.scriptFiles.zip(plugin.readScriptTexts()).forEach { (file, source) ->
            Log.debug("Launch script: $file")
            js.exec(file, source)
            Log.debug("Finished script: $file")
        }

        if (plugin.outputRawEntry) {
            val lines = js.getJson<List<String>>("output")
            Log.info("Plugin execution got raw lines:\n" + lines.joinToString("\n"))
            TemplatePluginResult.Raw(lines)
        } else {
            val entries = js.getJson<List<Entry>>("output")
            Log.info("Plugin execution got entries:\n" + entries.joinToString("\n"))
            TemplatePluginResult.Parsed(entries)
        }
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
