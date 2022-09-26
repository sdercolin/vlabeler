package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.postApplyLabelerConf
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.matchGroups
import com.sdercolin.vlabeler.util.orEmpty
import com.sdercolin.vlabeler.util.replaceWithVariables
import com.sdercolin.vlabeler.util.roundToDecimalDigit
import java.io.File

fun fromRawLabels(
    sources: List<String>,
    inputFile: File?,
    labelerConf: LabelerConf,
    labelerParams: ParamMap?,
    sampleNames: List<String>,
): List<Entry> {
    val parser = labelerConf.parser
    val extractor = Regex(parser.extractionPattern)
    val js = JavaScript()
    js.setJson("params", labelerParams.orEmpty().resolve(project = null, js = js))
    val entriesBySampleName = sources.mapNotNull { source ->
        runCatching {
            val groups = source.matchGroups(extractor)
            parser.variableNames.mapIndexed { i, name ->
                js.set(name, groups[i])
            }
            js.set("inputFileName", inputFile?.nameWithoutExtension)
            js.set("sampleNames", sampleNames)
            val script = parser.scripts.joinToString("\n")
            js.eval(script)

            // when "sample" is not set, using the first sample's name
            // because the label file will not contain the sample name if there is only one
            val sampleName = js.getOrNull<String>("sample")?.takeUnless { it.isEmpty() }
                ?: sampleNames.first()

            // require name, otherwise ignore the entry
            val name = js.get<String>("name")
            require(name.isNotEmpty()) { "Cannot get `name` from parser in `$source`" }

            // optional start, end, points
            val start = js.getOrNull<Double>("start")?.toFloat()
            val end = js.getOrNull<Double>("end")?.toFloat()
            val points = js.getOrNull<List<Double>>("points")?.map { it.toFloat() } ?: listOf()

            // optional extras
            val extras = js.getJsonOrNull("extras") ?: labelerConf.defaultExtras

            if (start == null || end == null || points.size != labelerConf.fields.size) {
                // use default except name if data size is not enough
                Entry.fromDefaultValues(sampleName, name, labelerConf).also {
                    Log.info("Entry parse failed, fallback to default: $it")
                }
            } else {
                Entry(sample = sampleName, name = name, start = start, end = end, points = points, extras = extras)
            }
        }.getOrElse {
            Log.debug(it)
            null
        }
    }

    js.close()

    return entriesBySampleName.postApplyLabelerConf(labelerConf)
}

fun Project.toRawLabels(): String {
    val js = JavaScript()
    js.setJson("params", labelerParams?.toParamMap().orEmpty().resolve(project = null, js = js))
    val lines = entries
        .map { entry ->
            val fields = labelerConf.getFieldMap(entry)
            val extras = labelerConf.getExtraMap(entry)
            val properties = labelerConf.getPropertyMap(fields, extras, js)
            val variables: Map<String, Any> =
                fields.mapValues { it.value.roundToDecimalDigit(labelerConf.decimalDigit) } +
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

private fun LabelerConf.getFieldMap(entry: Entry) =
    mapOf(
        "start" to entry.start,
        "end" to entry.end,
    ) + fields.mapIndexed { index, field ->
        field.name to entry.points[index]
    }.toMap()

private fun LabelerConf.getExtraMap(entry: Entry) = extraFieldNames.mapIndexed { index, name ->
    name to entry.extras[index]
}.toMap()

private fun LabelerConf.getPropertyBaseMap(fields: Map<String, Float>, extras: Map<String, String>, js: JavaScript) =
    properties.associateWith {
        val expression = it.value.replaceWithVariables(fields + extras)
        js.eval(expression)!!.asDouble()
    }

private fun LabelerConf.getPropertyMap(fields: Map<String, Float>, extras: Map<String, String>, js: JavaScript) =
    getPropertyBaseMap(fields, extras, js).mapKeys { it.key.name }

fun LabelerConf.getPropertyMap(entry: Entry, js: JavaScript) =
    getPropertyBaseMap(getFieldMap(entry), getExtraMap(entry), js)
