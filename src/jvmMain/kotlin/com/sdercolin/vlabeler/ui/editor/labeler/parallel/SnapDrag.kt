package com.sdercolin.vlabeler.ui.editor.labeler.parallel

import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryConverter
import kotlin.math.abs

data class SnapTarget(
    val moduleName: String,
    val entryIndex: Int,
)

class SnapDrag(project: Project, lengthInPixel: Float, entryConverter: EntryConverter) {

    private val map: Map<Float, List<SnapTarget>> = project.modules
        .filter { it.isParallelTo(project.currentModule) }
        .flatMap { module ->
            module.entryGroups.flatMap { (_, entryGroup) ->
                entryGroup.zipWithNext().map { (entry, _) ->
                    val pixel = entryConverter.convertToPixel(entry.end).coerceAtMost(lengthInPixel)
                    pixel to SnapTarget(
                        moduleName = module.name,
                        entryIndex = entry.index,
                    )
                }
            }
        }
        .groupBy({ it.first }, { it.second })
        .toList()
        .sortedBy { it.first }
        .toMap()

    private var mapInRange: Map<Float, List<SnapTarget>> = map

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
        for ((key, targets) in mapInRange) {
            val count = if (current == key) targets.size - 1 else targets.size
            if (count <= 0) continue
            if (abs(key - position) <= SNAP_DISTANCE) {
                return key
            }
        }
        return position
    }

    fun getSnapTargets(position: Float): List<SnapTarget> {
        val candidates = map.entries
            .filter { abs(it.key - position) <= CASCADE_MATCH_DISTANCE }
            .flatMap { entry -> entry.value.map { target -> target to entry.key } }
        return candidates
            .groupBy { it.first.moduleName }
            .mapNotNull { (_, targetsWithKeys) ->
                targetsWithKeys.minByOrNull { abs(it.second - position) }?.first
            }
    }

    companion object {
        private const val SNAP_DISTANCE = 10f
        private const val CASCADE_MATCH_DISTANCE = 1f
    }
}
