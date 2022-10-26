package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.ModuleDefinition
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.postApplyLabelerConf
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.execResource
import com.sdercolin.vlabeler.util.matchGroups
import com.sdercolin.vlabeler.util.readTextByEncoding
import com.sdercolin.vlabeler.util.replaceWithVariables
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.roundToDecimalDigit
import java.io.File

fun moduleFromRawLabels(
    sources: List<String>,
    inputFile: File?,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    sampleFiles: List<File>,
    encoding: String,
    legacyMode: Boolean,
): List<Entry> {
    val parser = labelerConf.parser
    val extractor = Regex(parser.extractionPattern)
    val inputFileNames = listOfNotNull(inputFile?.name)
    val sampleFileNames = sampleFiles.map { it.name }
    val js = prepareJsForParsing(labelerParams, inputFileNames, sampleFileNames, encoding)
    val entries = sources.mapIndexedNotNull { index, source ->
        if (source.isBlank()) return@mapIndexedNotNull null
        val errorMessageSuffix = "in file ${inputFile?.absolutePath}, line ${index + 1}: $source"
        runCatching {
            val groups = source.matchGroups(extractor)
            require(groups.size >= parser.variableNames.size) {
                "Extracted groups less than required $errorMessageSuffix"
            }

            parser.variableNames.mapIndexed { i, name ->
                js.set(name, groups.getOrNull(i))
            }
            val script = parser.scripts.joinToString("\n")
            js.eval(script)

            if (legacyMode) {
                // when "sample" is not set, using the first sample's name
                // because the label file will not contain the sample name if there is only one
                val sampleName = js.getOrNull<String>("sample")
                    ?.takeUnless { it.isEmpty() }
                    ?: sampleFiles.first().name

                // require name, otherwise ignore the entry
                val name = js.get<String>("name")
                require(name.isNotEmpty()) { "Cannot get `name` from parser $errorMessageSuffix" }

                // optional start, end, points
                val start = js.getOrNull<Double>("start")?.toFloat()
                val end = js.getOrNull<Double>("end")?.toFloat()
                val points = js.getOrNull<List<Double>>("points")?.map { it.toFloat() } ?: listOf()

                // optional extras
                val extras = js.getJsonOrNull("extras") ?: labelerConf.defaultExtras

                if (start == null || end == null || points.size != labelerConf.fields.size) {
                    // use default except name if data size is not enough
                    Entry.fromDefaultValues(sampleName, name, labelerConf).also {
                        Log.info("Entry parse failed, fallback to default: $it, $errorMessageSuffix")
                    }
                } else {
                    Entry(sample = sampleName, name = name, start = start, end = end, points = points, extras = extras)
                }
            } else {
                js.getJson("entry")
            }
        }.getOrElse {
            Log.debug(it)
            null
        }
    }

    js.close()

    return entries.postApplyLabelerConf(labelerConf)
}

fun moduleGroupFromRawLabels(
    definitionGroup: List<ModuleDefinition>,
    labelerConf: LabelerConf,
    labelerParams: ParamMap,
    encoding: String,
): List<List<Entry>> {
    val inputFileNames = definitionGroup.first().inputFiles.orEmpty().map { it.name }
    val sampleFileNames = definitionGroup.first().sampleFiles.map { it.name }

    if (inputFileNames.isEmpty()) {
        Log.info("No input files, fallback to default values")
        return definitionGroup.map {
            sampleFileNames.map { sampleName ->
                Entry.fromDefaultValues(sampleName, sampleName.substringBeforeLast('.'), labelerConf)
            }
        }
    }

    val js = prepareJsForParsing(labelerParams, inputFileNames, sampleFileNames, encoding)
    val inputs = requireNotNull(definitionGroup.first().inputFiles).map { it.readTextByEncoding(encoding).lines() }
    js.setJson("moduleNames", definitionGroup.map { it.name })
    js.setJson("inputs", inputs)

    val script = labelerConf.parser.scripts.joinToString("\n")
    js.eval(script)

    val result = js.getJson<List<List<Entry>>>("modules")
    js.close()

    return result
}

private fun prepareJsForParsing(
    labelerParams: ParamMap,
    inputFileNames: List<String>,
    sampleFileNames: List<String>,
    encoding: String,
): JavaScript {
    val js = JavaScript()
    listOf(
        Resources.classEntryJs,
        Resources.expectedErrorJs,
        Resources.envJs,
        Resources.fileJs,
    ).forEach { js.execResource(it) }
    js.set("debug", isDebug)
    js.setJson("params", labelerParams.resolve(project = null, js = js))
    js.setJson("inputFileNames", inputFileNames)
    js.setJson("sampleFileNames", sampleFileNames)
    js.set("encoding", encoding)
    return js
}

fun Project.modulesToRawLabels(moduleIndexes: List<Int>): String {
    val js = prepareJsForWriting()
    val relatedModules = moduleIndexes.map { modules[it] }
    js.setJson("moduleNames", relatedModules.map { it.name })
    js.setJson("modules", relatedModules.map { it.entries })
    val scripts = labelerConf.writer.scripts
    requireNotNull(scripts) { "Writer scripts are required when scope is Scope.Modules" }

    js.eval(scripts.joinToString("\n"))
    val result = js.get<String>("output")
    js.close()
    return result
}

fun Project.singleModuleToRawLabels(moduleIndex: Int): String {
    val js = prepareJsForWriting()
    val lines = modules[moduleIndex].entries
        .map { entry ->
            val fields = labelerConf.getFieldMap(entry)
            val extras = labelerConf.getExtraMap(entry)
            val properties = labelerConf.getPropertyMap(labelerConf, entry, fields, extras, js)
            val variables: Map<String, Any> =
                fields.mapValues { (it.value as? Float)?.roundToDecimalDigit(labelerConf.decimalDigit) ?: it.value } +
                    // if a name is shared in fields and properties, its value will be overwritten by properties
                    // See source of Kotlin's `fun Map<out K, V>.plus(map: Map<out K, V>)`
                    properties.mapValues { it.value.roundToDecimalDigit(labelerConf.decimalDigit) } +
                    extras +
                    mapOf(
                        "sample" to entry.sample,
                        "name" to entry.name,
                    )
            val scripts = labelerConf.writer.scripts
            if (scripts != null) {
                for (variable in variables) {
                    js.set(variable.key, variable.value)
                }
                js.eval(scripts.joinToString("\n"))
                js.get("output")
            } else {
                val format = labelerConf.writer.format!!
                format.replaceWithVariables(variables)
            }
        }
    js.close()
    return lines.joinToString("\n")
}

private fun Project.prepareJsForWriting(): JavaScript {
    val js = JavaScript()
    listOf(
        Resources.classEntryJs,
        Resources.expectedErrorJs,
        Resources.envJs,
        Resources.fileJs,
    ).forEach { js.execResource(it) }
    js.set("debug", isDebug)
    js.setJson("params", labelerParams.resolve(labelerConf).resolve(project = null, js = js))
    return js
}

private fun LabelerConf.getFieldMap(entry: Entry) =
    mapOf(
        "start" to entry.start,
        "end" to entry.end,
        "needSync" to entry.needSync,
    ) + fields.mapIndexed { index, field ->
        field.name to entry.points[index]
    }.toMap()

private fun LabelerConf.getExtraMap(entry: Entry) = extraFieldNames.mapIndexed { index, name ->
    name to entry.extras[index]
}.toMap()

private fun LabelerConf.getPropertyBaseMap(
    labelerConf: LabelerConf,
    entry: Entry,
    fields: Map<String, Any>,
    extras: Map<String, String>,
    js: JavaScript,
) =
    properties.associateWith {
        if (it.valueGetter != null) {
            js.setJson("entry", entry)
            js.eval(it.valueGetter.joinToString("\n"))
            js.get<Double>("value").roundToDecimalDigit(labelerConf.decimalDigit)
        } else {
            requireNotNull(it.value)
            // for backward compatibility
            val expression = it.value.replaceWithVariables(fields + extras)
            js.eval(expression)!!.asDouble()
        }
    }

private fun LabelerConf.getPropertyMap(
    labelerConf: LabelerConf,
    entry: Entry,
    fields: Map<String, Any>,
    extras: Map<String, String>,
    js: JavaScript,
) =
    getPropertyBaseMap(labelerConf, entry, fields, extras, js).mapKeys { it.key.name }

fun LabelerConf.getPropertyMap(labelerConf: LabelerConf, entry: Entry, js: JavaScript) =
    getPropertyBaseMap(labelerConf, entry, getFieldMap(entry), getExtraMap(entry), js)
