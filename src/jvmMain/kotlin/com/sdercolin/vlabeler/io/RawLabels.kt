package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.Python
import com.sdercolin.vlabeler.util.matchGroups

fun parseRawLabels(sources: List<String>, parser: LabelerConf.Parser): Map<String, List<Entry>> {
    val extractor = Regex(parser.extractionPattern)
    return sources.map { source ->
        val groups = source.matchGroups(extractor)
        val python = Python()
        parser.variableNames.mapIndexed { i, name ->
            python.set(name, groups[i])
        }
        val script = parser.parsingScript.joinToString("\n")
        python.exec(script)
        val name = python.get<String>("name")
        val start = python.get<Float>("start")
        val end = python.get<Float>("end")
        val points = python.get<List<Float>>("points")
        val sampleName = python.get<String>("sample")
        sampleName to Entry(name = name, start = start, end = end, points = points)
    }
        .groupBy { it.first }
        .map { group -> group.key to group.value.map { it.second } }
        .toMap()
}
