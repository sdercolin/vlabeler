package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import kotlin.math.absoluteValue

class MarkerState(
    val entries: List<IndexedEntry>,
    val entriesInSample: List<IndexedEntry>,
    val labelerConf: LabelerConf,
    val canvasParams: CanvasParams,
    val sampleLengthMillis: Float,
    val entryConverter: EntryConverter,
    val entriesInPixel: List<EntryInPixel>,
    val leftBorder: Float,
    val rightBorder: Float,
    val cursorState: MutableState<MarkerCursorState>,
    val scissorsState: MutableState<MarkerScissorsState?>,
    val canvasHeightState: MutableState<Float>,
    val waveformsHeightRatio: Float
) {
    val entryBorders: List<Float> = entriesInPixel.fold<EntryInPixel, List<Float>>(listOf()) { acc, entryInPixel ->
        val lastEntryEnd = acc.lastOrNull()
        if (lastEntryEnd == null) {
            acc + listOf(entryInPixel.start, entryInPixel.end)
        } else {
            require(lastEntryEnd == entryInPixel.start) {
                "Cannot draw non-continuous entries with $entryInPixel"
            }
            acc + listOf(entryInPixel.end)
        }
    }.drop(1).dropLast(1)

    private val startInPixel = entriesInPixel.first().start
    private val endInPixel = entriesInPixel.last().end
    private val middlePointsInPixel = entriesInPixel.flatMapIndexed { index: Int, entryInPixel: EntryInPixel ->
        listOfNotNull(entryBorders.getOrNull(index - 1)) + entryInPixel.points
    }
    private val middlePointsInPixelSorted = middlePointsInPixel.sorted()

    private val hoveredIndexSet = hashSetOf<Int>()
    private var isLabelHovered: Boolean by mutableStateOf(false)

    fun isBorderIndex(index: Int): Boolean {
        if (index < 0) return false
        return (index + 1) % (labelerConf.fields.size + 1) == 0
    }

    fun getEntryIndexesByBorderIndex(index: Int): Pair<Int, Int> {
        require(isBorderIndex((index)))
        val second = (index + 1) / (labelerConf.fields.size + 1)
        return second - 1 to second
    }

    private fun getEntryIndexByPointIndex(index: Int): Int {
        require(index >= 0 && isBorderIndex(index).not())
        return index / (labelerConf.fields.size + 1)
    }

    private fun getFieldIndexByPointIndex(index: Int): Int {
        require(index >= 0 && isBorderIndex(index).not())
        return index % (labelerConf.fields.size + 1)
    }

    private fun getPointPosition(index: Int): Float = when (index) {
        MarkerCursorState.StartPointIndex -> startInPixel
        MarkerCursorState.EndPointIndex -> endInPixel
        else -> middlePointsInPixel[index]
    }

    fun getLockedDraggedEntries(
        pointIndex: Int,
        x: Float,
        leftBorder: Float,
        rightBorder: Float
    ): List<EntryInPixel> {
        if (pointIndex == MarkerCursorState.NonePointIndex) return entriesInPixel
        val dxMin = leftBorder - startInPixel
        val dxMax = rightBorder - endInPixel
        val dx = (x - getPointPosition(pointIndex)).coerceIn(dxMin, dxMax)
        return entriesInPixel.map { it.moved(dx) }
    }

    fun getDraggedEntries(
        pointIndex: Int,
        x: Float,
        leftBorder: Float,
        rightBorder: Float,
        conf: LabelerConf
    ): List<EntryInPixel> {
        val entries = entriesInPixel.toMutableList()
        when {
            pointIndex == MarkerCursorState.NonePointIndex -> Unit
            pointIndex == MarkerCursorState.StartPointIndex -> {
                val max = middlePointsInPixelSorted.firstOrNull() ?: endInPixel
                val firstUpdated = entries.first().copy(start = x.coerceIn(leftBorder, max))
                entries[0] = firstUpdated
            }
            pointIndex == MarkerCursorState.EndPointIndex -> {
                val min = middlePointsInPixelSorted.lastOrNull() ?: startInPixel
                val lastUpdated = entries.last().copy(end = x.coerceIn(min, rightBorder - 1))
                entries[entries.lastIndex] = lastUpdated
            }
            isBorderIndex(pointIndex) -> {
                val (firstEntryIndex, secondEntryIndex) = getEntryIndexesByBorderIndex(pointIndex)
                val min = entries[firstEntryIndex].run { points.maxOrNull() ?: start }
                val max = entries[secondEntryIndex].run { points.minOrNull() ?: end }
                val newBorder = x.coerceIn(min, max)
                entries[firstEntryIndex] = entries[firstEntryIndex].copy(end = newBorder)
                entries[secondEntryIndex] = entries[secondEntryIndex].copy(start = newBorder)
            }
            else -> {
                val entryIndex = getEntryIndexByPointIndex(pointIndex)
                val entry = entries[entryIndex]
                val points = entry.points

                val constraints = conf.connectedConstraints
                val min = constraints.filter { it.second == pointIndex }
                    .maxOfOrNull { points[it.first] }
                    ?: entry.start
                val max = constraints.filter { it.first == pointIndex }
                    .minOfOrNull { points[it.second] }
                    ?: entry.end
                val newPoints = points.toMutableList()
                val pointInsideIndex = pointIndex % (labelerConf.fields.size + 1)
                newPoints[pointInsideIndex] = x.coerceIn(min, max)
                val newEntry = entry.copy(points = newPoints)
                entries[entryIndex] = newEntry
            }
        }
        return entries
    }

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
        if (isLabelHovered) return MarkerCursorState.NonePointIndex

        // end
        if ((endInPixel - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x >= endInPixel) return MarkerCursorState.EndPointIndex
            val prev = middlePointsInPixelSorted.lastOrNull() ?: startInPixel
            if (endInPixel - x <= x - prev) return MarkerCursorState.EndPointIndex
        }

        // start
        if ((startInPixel - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x <= startInPixel) return MarkerCursorState.StartPointIndex
            val next = middlePointsInPixelSorted.firstOrNull() ?: endInPixel
            if (x - startInPixel <= next - x) return MarkerCursorState.StartPointIndex
        }

        // other points
        val pointsSorted = middlePointsInPixel.withIndex().sortedBy { it.value }

        // label part
        for ((index, value) in pointsSorted.reversed()) {
            if (isBorderIndex(index)) continue
            val fieldIndex = getFieldIndexByPointIndex(index)
            val centerY = canvasHeight * waveformsHeightRatio *
                (1 - conf.fields[fieldIndex].height) - with(density) { labelShiftUp.toPx() }
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

        // line part
        (pointsSorted + listOf(IndexedValue(MarkerCursorState.EndPointIndex, endInPixel))).zipWithNext()
            .reversed()
            .forEach { (current, next) ->
                val radius = if (isBorderIndex(current.index)) NearRadiusStartOrEnd else NearRadiusCustom
                if ((current.value - x).absoluteValue > radius) return@forEach
                if (current.value == next.value || x - current.value <= next.value - x) {
                    val heightRatio = if (isBorderIndex(current.index)) {
                        1f
                    } else {
                        val fieldIndex = getFieldIndexByPointIndex(current.index)
                        conf.fields[fieldIndex].height
                    }
                    val top = canvasHeight * waveformsHeightRatio * (1 - heightRatio)
                    if (y >= top) return current.index
                }
            }

        return MarkerCursorState.NonePointIndex
    }

    fun getClickedAudioRange(
        x: Float,
        leftBorder: Float,
        rightBorder: Float
    ): Pair<Float?, Float?>? {
        val borders =
            (listOf(leftBorder, rightBorder, startInPixel, endInPixel) + middlePointsInPixel).distinct().sorted()
        if (x < borders.first()) return null to borders.first()
        if (x > borders.last()) return borders.last() to null
        for (range in borders.zipWithNext()) {
            if (x > range.first && x < range.second) {
                return range
            }
        }
        return null
    }

    fun isValidCutPosition(position: Float) = entriesInPixel.any { it.isValidCutPosition(position) }

    fun getEntryIndexByCutPosition(position: Float) = entriesInPixel.first {
        it.isValidCutPosition(position)
    }.index

    fun onLabelHovered(index: Int, hovered: Boolean) {
        if (hovered) {
            hoveredIndexSet.add(index)
        } else {
            hoveredIndexSet.remove(index)
        }
        isLabelHovered = hoveredIndexSet.isNotEmpty()
    }

    companion object {
        private const val NearRadiusStartOrEnd = 20f
        private const val NearRadiusCustom = 5f
    }
}

@Composable
fun rememberMarkerState(
    sampleInfo: SampleInfo,
    canvasParams: CanvasParams,
    editorState: EditorState,
    appState: AppState
): MarkerState {
    val sampleRate = sampleInfo.sampleRate
    val sampleLengthMillis = sampleInfo.lengthMillis
    val entries = editorState.editedEntries
    val project = editorState.project
    val allEntriesInCurrentGroup = remember(entries) { project.getEntriesInGroupForEditing() }
    val labelerConf = project.labelerConf
    val entryConverter = remember(sampleInfo.sampleRate, canvasParams.resolution) {
        EntryConverter(sampleInfo.sampleRate, canvasParams.resolution)
    }
    val entriesInPixel = remember(entries, canvasParams.lengthInPixel, sampleLengthMillis) {
        entries.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val entriesInSampleInPixel = remember(entriesInPixel) {
        allEntriesInCurrentGroup.map {
            entryConverter.convertToPixel(it, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val leftBorder = remember(entriesInPixel) {
        val previousEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getPreviousOrNull { it.index == entriesInPixel.first().index }
        } else null
        previousEntry?.start ?: 0f
    }
    val rightBorder = remember(entriesInPixel) {
        val nextEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getNextOrNull { it.index == entriesInPixel.last().index }
        } else null
        nextEntry?.end ?: canvasParams.lengthInPixel.toFloat()
    }
    val cursorState = remember { mutableStateOf(MarkerCursorState()) }
    val scissorsState = remember { mutableStateOf<MarkerScissorsState?>(null) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val waveformsHeightRatio = remember(appState.appConf.painter.spectrogram) {
        val spectrogram = appState.appConf.painter.spectrogram
        val totalWeight = 1f + if (spectrogram.enabled) spectrogram.heightWeight else 0f
        1f / totalWeight
    }

    return remember(
        sampleRate,
        sampleLengthMillis,
        entries,
        allEntriesInCurrentGroup,
        labelerConf,
        canvasParams,
        entryConverter,
        entriesInPixel,
        entriesInSampleInPixel,
        leftBorder,
        rightBorder,
        cursorState,
        scissorsState,
        canvasHeightState,
        waveformsHeightRatio
    ) {
        MarkerState(
            entries,
            allEntriesInCurrentGroup,
            labelerConf,
            canvasParams,
            sampleLengthMillis,
            entryConverter,
            entriesInPixel,
            leftBorder,
            rightBorder,
            cursorState,
            scissorsState,
            canvasHeightState,
            waveformsHeightRatio
        )
    }
}
