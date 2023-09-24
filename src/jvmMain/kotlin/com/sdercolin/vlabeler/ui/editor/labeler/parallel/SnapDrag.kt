package com.sdercolin.vlabeler.ui.editor.labeler.parallel

import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import kotlin.math.abs

class SnapDrag(project: Project, lengthInPixel: Float, entryConverter: EntryConverter) {

    private val map = project.modules
        .filter { it.isParallelTo(project.currentModule) }
        .flatMap { module ->
            module.entryGroups.flatMap { (_, entryGroup) ->
                entryGroup.zipWithNext().map { it.first.end }
            }
        }
        .asSequence()
        .map { entryConverter.convertToPixel(it).coerceAtMost(lengthInPixel) }
        .groupBy { it }
        .toList()
        .sortedBy { it.first }
        .associate { it.first to it.second.size }

    private var mapInRange: Map<Float, Int> = map

    private var min: Float? = null
    private var max: Float? = null
    private var current: Float? = null

    fun update(current: Float, min: Float? = null, max: Float? = null) {
        if (min == this.min && max == this.max && current == this.current) return
        mapInRange =
            map.filter { it.key > (min ?: Float.NEGATIVE_INFINITY) && it.key < (max ?: Float.POSITIVE_INFINITY) }
        this.min = min
        this.max = max
        this.current = current
    }

    fun snap(position: Float): Float {
        for ((key, count) in mapInRange) {
            val countToUse = if (current == key) count - 1 else count
            if (countToUse <= 0) continue
            if (abs(key - position) <= SNAP_DISTANCE) {
                return key
            }
        }
        return position
    }

    companion object {
        private const val SNAP_DISTANCE = 10f
    }
}
