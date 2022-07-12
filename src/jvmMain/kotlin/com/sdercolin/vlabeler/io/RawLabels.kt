package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.mergeEntriesWithSampleNames
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.Python
import com.sdercolin.vlabeler.util.matchGroups
import com.sdercolin.vlabeler.util.replaceWithVariables
import com.sdercolin.vlabeler.util.roundToDecimalDigit

fun fromRawLabels(
    sources: List<String>,
    labelerConf: LabelerConf,
    sampleNames: List<String>
): List<Entry> {
    val parser = labelerConf.parser
    val extractor = Regex(parser.extractionPattern)
    val js = JavaScript()
    val entriesBySampleName = sources.mapNotNull { source ->
        runCatching {
            val groups = source.matchGroups(extractor)
            parser.variableNames.mapIndexed { i, name ->
                js.set(name, groups[i])
            }
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

            // optional extra
            val extra = labelerConf.extraFieldNames.mapIndexed { index, extraName ->
                js.getOrNull<Any>(extraName)?.toString() ?: labelerConf.defaultExtras[index]
            }

            if (start == null || end == null || points.size != labelerConf.fields.size) {
                // use default except name if data size is not enough
                Entry.fromDefaultValues(sampleName, name, labelerConf).also {
                    Log.info("Entry parse failed, fallback to default: $it")
                }
            } else {
                Entry(sample = sampleName, name = name, start = start, end = end, points = points, extra = extra)
            }
        }.getOrElse {
            Log.debug(it)
            null
        }
    }

    js.close()

    return mergeEntriesWithSampleNames(labelerConf, entriesBySampleName, sampleNames)
}

fun Project.toRawLabels(): String {
    val python = Python()
    val lines = entries
        .map { entry ->
            val fields = labelerConf.getFieldMap(entry)
            val extra = labelerConf.getExtraMap(entry)
            val properties = labelerConf.getPropertyMap(fields, extra, python)
            val variables: Map<String, Any> =
                fields.mapValues { it.value.roundToDecimalDigit(labelerConf.decimalDigit) } +
                    // if a name is shared in fields and properties, its value will be overwritten by properties
                    // See source of Kotlin's `fun Map<out K, V>.plus(map: Map<out K, V>)`
                    properties.mapValues { it.value.roundToDecimalDigit(labelerConf.decimalDigit) } +
                    extra +
                    mapOf(
                        "sample" to entry.sample,
                        "name" to entry.name
                    )
            val scripts = labelerConf.writer.scripts
            if (scripts != null) {
                for (variable in variables) {
                    python.set(variable.key, variable.value)
                }
                python.exec(scripts.joinToString("\n"))
                python.get("output")
            } else {
                val format = labelerConf.writer.format!!
                format.replaceWithVariables(variables)
            }
        }
    python.close()
    return lines.joinToString("\n")
}

private fun LabelerConf.getFieldMap(entry: Entry) =
    mapOf(
        "start" to entry.start,
        "end" to entry.end
    ) + fields.mapIndexed { index, field ->
        field.name to entry.points[index]
    }.toMap()

private fun LabelerConf.getExtraMap(entry: Entry) = extraFieldNames.mapIndexed { index, name ->
    name to entry.extra[index]
}.toMap()

private fun LabelerConf.getPropertyBaseMap(fields: Map<String, Float>, extras: Map<String, String>, python: Python) =
    properties.associateWith {
        it.value.replaceWithVariables(fields + extras).let(python::eval)
    }

private fun LabelerConf.getPropertyBaseMap(fields: Map<String, Float>, extras: Map<String, String>, js: JavaScript) =
    properties.associateWith {
        val expression = it.value.replaceWithVariables(fields + extras)
        js.eval(expression)!!.asDouble()
    }

private fun LabelerConf.getPropertyMap(fields: Map<String, Float>, extras: Map<String, String>, python: Python) =
    getPropertyBaseMap(fields, extras, python).mapKeys { it.key.name }

fun LabelerConf.getPropertyMap(entry: Entry, js: JavaScript) =
    getPropertyBaseMap(getFieldMap(entry), getExtraMap(entry), js)
