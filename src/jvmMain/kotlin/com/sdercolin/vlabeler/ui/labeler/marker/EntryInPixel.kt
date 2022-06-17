package com.sdercolin.vlabeler.ui.labeler.marker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.sdercolin.vlabeler.model.LabelerConf
import kotlin.math.absoluteValue

@Immutable
data class EntryInPixel(
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>
) {

    private val pointsSorted = points.sorted()

    fun getPoint(index: Int): Float = when (index) {
        MarkerState.StartPointIndex -> start
        MarkerState.EndPointIndex -> end
        else -> getCustomPoint(index)
    }

    fun getCustomPoint(index: Int): Float = points.getOrNull(index) ?: points.lastOrNull() ?: start

    fun getPointIndexForHovering(
        x: Float,
        y: Float,
        conf: LabelerConf,
        canvasHeight: Float,
        waveformsHeightRatio: Float,
        density: Density,
        labelSize: DpSize,
        labelShiftUp: Dp
    ): Int {
        // end
        if ((end - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x >= end) return MarkerState.EndPointIndex
            val prev = pointsSorted.lastOrNull() ?: start
            if (end - x <= x - prev) return MarkerState.EndPointIndex
        }

        // start
        if ((start - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x <= start) return MarkerState.StartPointIndex
            val next = pointsSorted.firstOrNull() ?: end
            if (x - start <= next - x) return MarkerState.StartPointIndex
        }

        // other points
        val pointsSorted = points.withIndex().sortedBy { it.value }
        for ((index, value) in pointsSorted) {
            // label part
            val centerY = canvasHeight * waveformsHeightRatio *
                    (1 - conf.fields[index].height) - with(density) { labelShiftUp.toPx() }
            val height = with(density) { labelSize.height.toPx() }
            val top = centerY - 0.5 * height
            val bottom = centerY + 0.5 * height
            val width = with(density) { labelSize.width.toPx() }
            val left = value - 0.5f * width
            val right = value + 0.5f * width
            if (x in left..right && y in top..bottom) {
                return index
            }
        }
        for ((current, next) in (pointsSorted + listOf(IndexedValue(MarkerState.EndPointIndex, end))).zipWithNext()) {
            // line part
            if ((current.value - x).absoluteValue > NearRadiusCustom) continue
            if (current.value == next.value || x - current.value <= next.value - x) {
                val top = canvasHeight * waveformsHeightRatio * (1 - conf.fields[current.index].height)
                if (y >= top) return current.index
            }
        }

        return MarkerState.NonePointIndex
    }

    fun drag(pointIndex: Int, x: Float, conf: LabelerConf, canvasWidthInPixel: Int): EntryInPixel =
        when (pointIndex) {
            MarkerState.NonePointIndex -> this
            MarkerState.StartPointIndex -> {
                val max = pointsSorted.firstOrNull() ?: end
                copy(start = x.coerceIn(0f, max))
            }
            MarkerState.EndPointIndex -> {
                val min = pointsSorted.lastOrNull() ?: start
                copy(end = x.coerceIn(min, canvasWidthInPixel.toFloat() - 1))
            }
            else -> {
                val constraints = conf.connectedConstraints
                val min = constraints.filter { it.second == pointIndex }
                    .maxOfOrNull { points[it.first] }
                    ?: start
                val max = constraints.filter { it.first == pointIndex }
                    .minOfOrNull { points[it.second] }
                    ?: end
                val newPoints = points.toMutableList()
                newPoints[pointIndex] = x.coerceIn(min, max)
                copy(points = newPoints)
            }
        }

    fun lockedDrag(pointIndex: Int, x: Float, canvasWidthInPixel: Int): EntryInPixel {
        if (pointIndex == MarkerState.NonePointIndex) return this
        val dxMin = -start
        val dxMax = canvasWidthInPixel.toFloat() - 1 - end
        val dx = (x - getPoint(pointIndex)).coerceIn(dxMin, dxMax)
        return copy(start = start + dx, end = end + dx, points = points.map { it + dx })
    }

    fun getClickedAudioRange(x: Float): Pair<Float?, Float?>? {
        val borders = (listOf(start, end) + points).distinct().sorted()
        if (x < borders.first()) return null to borders.first()
        if (x > borders.last()) return borders.last() to null
        for (range in borders.zipWithNext()) {
            if (x > range.first && x < range.second) {
                return range
            }
        }
        return null
    }


    companion object {
        private const val NearRadiusStartOrEnd = 10f
        private const val NearRadiusCustom = 5f
    }
}