package com.sdercolin.vlabeler.model

import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.Serializable

fun runMacroPlugin(
    plugin: Plugin,
    params: ParamMap,
    project: Project
): Project {
    val js = JavaScript(
        logHandler = Log.infoFileHandler,
        currentWorkingDirectory = requireNotNull(plugin.directory).absolutePath.toFile()
    )
    val resourceTexts = plugin.readResourceFiles()

    js.set("debug", isDebug)
    js.setJson("entries", project.entries)
    js.setJson("params", params.resolve(project, js))
    js.setJson("resources", resourceTexts)

    val entryDefCode = useResource("class_entry.js") { String(it.readAllBytes()) }
    val editedEntryDefCode = useResource("class_edited_entry.js") { String(it.readAllBytes()) }
    js.eval(entryDefCode)
    js.eval(editedEntryDefCode)

    plugin.scriptFiles.zip(plugin.readScriptTexts()).forEach { (file, source) ->
        Log.debug("Launch script: $file")
        js.exec(file, source)
        Log.debug("Finished script: $file")
    }

    val editedEntries = js.getJson<List<PluginEditedEntry>>("output")
    val newCount = editedEntries.count { it.originalIndex == null }
    val editedCount = editedEntries.count {
        if (it.originalIndex == null) {
            false
        } else {
            project.entries[it.originalIndex] != it.entry
        }
    }
    val removedCount = (project.entries.indices.toSet() - editedEntries.mapNotNull { it.originalIndex }.toSet()).size
    Log.info(
        buildString {
            appendLine("Plugin execution got edited entries:")
            appendLine("Total: " + editedEntries.size)
            appendLine("New: $newCount")
            appendLine("Edited: $editedCount")
            appendLine("Removed: $removedCount")
        }
    )
    js.close()
    return project.copy(entries = editedEntries.map { it.entry }).validate()
}

@Serializable
data class PluginEditedEntry(
    val originalIndex: Int?,
    val entry: Entry
)
