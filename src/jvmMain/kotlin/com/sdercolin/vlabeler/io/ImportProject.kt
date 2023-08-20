package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.json
import com.segment.analytics.kotlin.core.utilities.safeJsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A model class to contain imported module data.
 *
 * @property name Name of the module.
 * @property entries List of entries in the module.
 * @property pointSize Number of [Entry.points] defined by labeler.
 * @property extraSize Number of [Entry.extras] defined by labeler.
 * @property continuous Whether the labeler is continuous.
 * @property extension Extension of the raw label data, defined as [LabelerConf.extension].
 */
data class ImportedModule(
    val name: String,
    val entries: List<Entry>,
    val pointSize: Int,
    val extraSize: Int,
    val continuous: Boolean,
    val extension: String,
) {

    fun validate() = apply {
        require(
            entries.all { entry ->
                entry.points.size == pointSize && entry.extras.size == extraSize
            },
        )
    }

    fun isCompatibleWith(project: Project): Boolean {
        if (project.labelerConf.fields.size != pointSize) return false
        if (project.labelerConf.extraFields.size != extraSize) return false
        if (project.labelerConf.continuous != continuous) return false
        if (project.labelerConf.extension != extension) return false
        return true
    }
}

fun importModulesFromProject(projectText: String): List<ImportedModule> = runCatching {
    val root = json.parseToJsonElement(projectText)

    val modules = mutableListOf<ImportedModule>()

    val labeler = root.jsonObject.getValue("labelerConf").jsonObject
    val continuous = labeler.getValue("continuous").jsonPrimitive.boolean
    val extension = labeler.getValue("extension").jsonPrimitive.content

    // for project files before modules are introduced
    val entriesDirectlyUnderProject = root.jsonObject["entries"]
    if (entriesDirectlyUnderProject != null) {
        val entries = parseEntryArray(entriesDirectlyUnderProject)
        if (entries != null) {
            modules.add(
                ImportedModule(
                    name = "",
                    entries = entries,
                    pointSize = entries.first().points.size,
                    extraSize = entries.first().extras.size,
                    continuous = continuous,
                    extension = extension,
                ),
            )
        }
    }

    root.jsonObject["modules"]?.safeJsonArray?.forEach {
        val module = parseModule(it, continuous, extension)
        if (module != null) {
            modules.add(module)
        }
    }

    require(modules.isNotEmpty())

    modules.distinctBy { it.name }
}.getOrElse {
    Log.error(it)
    emptyList()
}

private fun parseModule(element: JsonElement, continuous: Boolean, extension: String): ImportedModule? = runCatching {
    val name = element.jsonObject.getValue("name").jsonPrimitive.content
    val entries = parseEntryArray(element.jsonObject.getValue("entries")) ?: return@runCatching null
    ImportedModule(
        name = name,
        entries = entries,
        pointSize = entries.first().points.size,
        extraSize = entries.first().extras.size,
        continuous = continuous,
        extension = extension,
    ).validate()
}.getOrElse {
    Log.error(it)
    null
}

private fun parseEntryArray(element: JsonElement): List<Entry>? = runCatching {
    element.jsonArray.mapNotNull { parseEntry(it) }
        .takeIf { it.isNotEmpty() }
}.getOrElse {
    Log.error(it)
    null
}

private fun parseEntry(element: JsonElement): Entry? = runCatching {
    val entry = json.decodeFromJsonElement<Entry>(element)
    val notes = if (element.jsonObject["notes"] == null) {
        // backward compatibility for "notes"
        element.jsonObject["meta"]?.let {
            json.decodeFromJsonElement<EntryNotes>(it)
        } ?: entry.notes
    } else {
        entry.notes
    }
    entry.copy(notes = notes)
}.getOrElse {
    Log.error(it)
    null
}
