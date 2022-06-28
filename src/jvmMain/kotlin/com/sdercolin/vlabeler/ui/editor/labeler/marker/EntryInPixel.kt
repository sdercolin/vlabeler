package com.sdercolin.vlabeler.ui.editor.labeler.marker

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
    val points: List<Float>,
    val extra: List<String>
) {

    private val pointsSorted = points.sorted()

    fun validate(canvasWidthInPixel: Int) = copy(
        start = start.coerceAtMost(canvasWidthInPixel.toFloat()),
        end = end.coerceAtMost(canvasWidthInPixel.toFloat()),
        points = points.map { it.coerceAtMost(canvasWidthInPixel.toFloat()) }
    )

    fun getPoint(index: Int): Float = when (index) {
        MarkerMouseState.StartPointIndex -> start
        MarkerMouseState.EndPointIndex -> end
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
            if (x >= end) return MarkerMouseState.EndPointIndex
            val prev = pointsSorted.lastOrNull() ?: start
            if (end - x <= x - prev) return MarkerMouseState.EndPointIndex
        }

        // start
        if ((start - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x <= start) return MarkerMouseState.StartPointIndex
            val next = pointsSorted.firstOrNull() ?: end
            if (x - start <= next - x) return MarkerMouseState.StartPointIndex
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
        for ((current, next) in (pointsSorted + listOf(IndexedValue(MarkerMouseState.EndPointIndex, end))).zipWithNext()) {
            // line part
            if ((current.value - x).absoluteValue > NearRadiusCustom) continue
            if (current.value == next.value || x - current.value <= next.value - x) {
                val top = canvasHeight * waveformsHeightRatio * (1 - conf.fields[current.index].height)
                if (y >= top) return current.index
            }
        }

        return MarkerMouseState.NonePointIndex
    }

    fun drag(
        pointIndex: Int,
        x: Float,
        leftBorder: Float,
        rightBorder: Float,
        conf: LabelerConf
    ): EntryInPixel =
        when (pointIndex) {
            MarkerMouseState.NonePointIndex -> this
            MarkerMouseState.StartPointIndex -> {
                val max = pointsSorted.firstOrNull() ?: end
                copy(start = x.coerceIn(leftBorder, max))
            }
            MarkerMouseState.EndPointIndex -> {
                val min = pointsSorted.lastOrNull() ?: start
                copy(end = x.coerceIn(min, rightBorder - 1))
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

    fun lockedDrag(
        pointIndex: Int,
        x: Float,
        leftBorder: Float,
        rightBorder: Float
    ): EntryInPixel {
        if (pointIndex == MarkerMouseState.NonePointIndex) return this
        val dxMin = leftBorder - start
        val dxMax = rightBorder - 1 - end
        val dx = (x - getPoint(pointIndex)).coerceIn(dxMin, dxMax)
        return copy(start = start + dx, end = end + dx, points = points.map { it + dx })
    }

    fun getClickedAudioRange(
        x: Float,
        leftBorder: Float,
        rightBorder: Float
    ): Pair<Float?, Float?>? {
        val borders = (listOf(leftBorder, rightBorder, start, end) + points).distinct().sorted()
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
        private const val NearRadiusStartOrEnd = 20f
        private const val NearRadiusCustom = 5f
    }
}
