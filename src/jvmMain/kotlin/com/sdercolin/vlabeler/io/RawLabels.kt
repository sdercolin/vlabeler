package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.Python
import com.sdercolin.vlabeler.util.matchGroups
import com.sdercolin.vlabeler.util.replaceWithVariables
import com.sdercolin.vlabeler.util.roundToDecimalDigit

fun fromRawLabels(sources: List<String>, labelerConf: LabelerConf): Map<String, List<Entry>> {
    val parser = labelerConf.parser
    val extractor = Regex(parser.extractionPattern)
    return sources.map { source ->
        val groups = source.matchGroups(extractor)
        val python = Python()
        parser.variableNames.mapIndexed { i, name ->
            python.set(name, groups[i])
        }
        val script = parser.scripts.joinToString("\n")
        python.exec(script)
        val name = python.get<String>("name")
        val start = python.get<Float>("start")
        val end = python.get<Float>("end")
        val points = python.get<List<Float>>("points")
        val sampleName = python.get<String>("sample")
        val extra = labelerConf.extraFieldNames.map { extraName ->
            python.get<Any>(extraName).toString()
        }
        sampleName to Entry(name = name, start = start, end = end, points = points, extra = extra)
    }
        .groupBy { it.first }
        .map { group -> group.key to group.value.map { it.second } }
        .toMap()
}

fun Project.toRawLabels(): String {
    val python = Python()
    val lines = entriesWithSampleName
        .map { (entry, sample) ->
            val fields = labelerConf.getFieldMap(entry)
            val extra = labelerConf.getExtraMap(entry)
            val properties = labelerConf.getPropertyMap(fields, python)
            val variables: Map<String, Any> = fields.mapValues { it.value.roundToDecimalDigit(3) } +
                properties.mapValues { it.value.roundToDecimalDigit(3) } +
                extra +
                mapOf(
                    "sample" to sample,
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

private fun LabelerConf.getPropertyMap(fields: Map<String, Float>, python: Python) =
    writer.properties.associate {
        val value = it.value.replaceWithVariables(fields).let(python::eval)
        it.name to value
    }
