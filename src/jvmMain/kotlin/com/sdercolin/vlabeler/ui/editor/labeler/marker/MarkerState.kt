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
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasState
import com.sdercolin.vlabeler.ui.editor.labeler.parallel.SnapDrag
import com.sdercolin.vlabeler.util.clear
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.update
import kotlin.math.absoluteValue

class MarkerState(
    val entries: List<IndexedEntry>,
    val entriesInCurrentGroup: List<IndexedEntry>,
    val labelerConf: LabelerConf,
    val canvasParams: CanvasParams,
    val sampleLengthMillis: Float,
    val entryConverter: EntryConverter,
    val entriesInPixel: List<EntryInPixel>,
    private val entriesInSampleInPixel: List<EntryInPixel>,
    val leftBorder: Float,
    val rightBorder: Float,
    val cursorState: MutableState<MarkerCursorState>,
    val scissorsState: MutableState<MarkerScissorsState?>,
    val panState: MutableState<MarkerPanState?>,
    val playbackState: MutableState<MarkerPlaybackState?>,
    val canvasHeightState: MutableState<Float>,
    val waveformsHeightRatio: Float,
    private val snapDrag: SnapDrag,
) {
    val entryBorders: List<Float> = entriesInPixel.fold<EntryInPixel, List<Float>>(listOf()) { acc, entryInPixel ->
        val lastEntryEnd = acc.lastOrNull()
        if (lastEntryEnd == null) {
            acc + listOf(entryInPixel.getActualStart(labelerConf), entryInPixel.getActualEnd(labelerConf))
        } else {
            require(lastEntryEnd == entryInPixel.start) {
                "Cannot draw non-continuous entries with $entryInPixel"
            }
            acc + listOf(entryInPixel.end)
        }
    }.drop(1).dropLast(1)

    private val startInPixel = entriesInPixel.first().getActualStart(labelerConf)
    private val endInPixel = entriesInPixel.last().getActualEnd(labelerConf)
    private val middlePointsInPixel = entriesInPixel.flatMapIndexed { index: Int, entryInPixel: EntryInPixel ->
        listOfNotNull(entryBorders.getOrNull(index - 1)) + entryInPixel.getActualMiddlePoints(labelerConf)
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

    fun getPointIndexAsSingleEntry(entryIndex: Int, pointIndex: Int): Int {
        val entryIndices = entriesInPixel.indices
        val startPointIndex = if (entryIndex == 0) {
            MarkerCursorState.START_POINT_INDEX
        } else {
            entryIndex * (labelerConf.fields.size + 1) - 1
        }
        val endPointIndex = if (entryIndex == entryIndices.last()) {
            MarkerCursorState.END_POINT_INDEX
        } else {
            startPointIndex + labelerConf.fields.size + 1
        }
        return when (pointIndex) {
            startPointIndex -> MarkerCursorState.START_POINT_INDEX
            endPointIndex -> MarkerCursorState.END_POINT_INDEX
            else -> {
                val fieldPointIndexes = if (startPointIndex == MarkerCursorState.START_POINT_INDEX) {
                    0 until labelerConf.fields.size
                } else {
                    (startPointIndex + 1) until (startPointIndex + 1 + labelerConf.fields.size)
                }
                fieldPointIndexes.indexOfFirst { it == pointIndex }
            }
        }
    }

    private fun getPointPosition(index: Int): Float = when (index) {
        MarkerCursorState.START_POINT_INDEX -> startInPixel
        MarkerCursorState.END_POINT_INDEX -> endInPixel
        else -> middlePointsInPixel[index]
    }

    fun getLockedDraggedEntries(
        pointIndex: Int,
        x: Float,
        forcedDrag: Boolean,
    ): List<EntryInPixel> {
        if (pointIndex == MarkerCursorState.NONE_POINT_INDEX) return entriesInPixel
        val minPixel = entriesInPixel.first().let {
            // use the min among all values
            minOf(it.start, it.points.minOrNull() ?: it.start)
        }
        val maxPixel = entriesInPixel.last().let {
            // use the max among all values
            maxOf(it.end, it.points.maxOrNull() ?: it.end)
        }
        return if (!forcedDrag) {
            val dxMin = leftBorder - minPixel
            val dxMax = (rightBorder - maxPixel - 1).coerceAtLeast(0f)
            if (dxMax <= dxMin) return entriesInPixel
            val dx = (x - getPointPosition(pointIndex)).coerceIn(dxMin, dxMax)
            entriesInPixel.map { it.moved(dx).validateImplicit(labelerConf) }
        } else {
            val currentX = getPointPosition(pointIndex)
            val dxMin = leftBorder - currentX
            val dxMax = (rightBorder - currentX - 1).coerceAtLeast(0f)
            if (dxMax <= dxMin) return entriesInPixel
            val dx = (x - getPointPosition(pointIndex)).coerceIn(dxMin, dxMax)
            entriesInPixel.map { it.moved(dx).collapsed(leftBorder, rightBorder).validateImplicit(labelerConf) }
        }
    }

    fun getDraggedEntries(
        pointIndex: Int,
        x: Float,
        forcedDrag: Boolean,
    ): List<EntryInPixel> {
        val entries = entriesInPixel.toMutableList()
        val currentEntries = entriesInSampleInPixel
        when {
            pointIndex == MarkerCursorState.NONE_POINT_INDEX -> Unit
            pointIndex == MarkerCursorState.START_POINT_INDEX -> {
                val max = if (!labelerConf.useImplicitStart) {
                    if (forcedDrag) rightBorder - 1 else middlePointsInPixelSorted.firstOrNull() ?: endInPixel
                } else {
                    val replacedStartIndex = labelerConf.fields.indexOfFirst { it.replaceStart }
                    labelerConf.connectedConstraints
                        .filter { it.first == replacedStartIndex }
                        .minOfOrNull { entries.first().points[it.second] }
                        ?: endInPixel
                }
                val start = x.coerceIn(leftBorder, max).runIf(!forcedDrag) {
                    snapDrag.update(current = currentEntries.first().getActualStart(labelerConf), max = max)
                    snapDrag.snap(this)
                }
                val firstUpdated = entries.first().setActualStart(labelerConf, start)
                entries[0] = firstUpdated
            }
            pointIndex == MarkerCursorState.END_POINT_INDEX -> {
                val min = if (!labelerConf.useImplicitEnd) {
                    if (forcedDrag) leftBorder else middlePointsInPixelSorted.lastOrNull() ?: startInPixel
                } else {
                    val replacedEndIndex = labelerConf.fields.indexOfFirst { it.replaceEnd }
                    labelerConf.connectedConstraints
                        .filter { it.second == replacedEndIndex }
                        .maxOfOrNull { entries.last().points[it.first] }
                        ?: startInPixel
                }
                val max = (rightBorder - 1).coerceAtLeast(min)
                val end = x.coerceIn(min, max).runIf(!forcedDrag) {
                    snapDrag.update(current = currentEntries.last().getActualEnd(labelerConf), min = min, max = max)
                    snapDrag.snap(this)
                }
                val lastUpdated = entries.last().setActualEnd(labelerConf, end)
                entries[entries.lastIndex] = lastUpdated
            }
            isBorderIndex(pointIndex) -> {
                val (firstEntryIndex, secondEntryIndex) = getEntryIndexesByBorderIndex(pointIndex)
                val min = if (forcedDrag) leftBorder else entries[firstEntryIndex].run { points.maxOrNull() ?: start }
                val max =
                    if (forcedDrag) rightBorder - 1 else entries[secondEntryIndex].run { points.minOrNull() ?: end }
                val newBorder = x.coerceIn(min, max).runIf(!forcedDrag) {
                    snapDrag.update(current = currentEntries[firstEntryIndex].end, min = min, max = max)
                    snapDrag.snap(this)
                }
                entries[firstEntryIndex] = entries[firstEntryIndex].copy(end = newBorder)
                entries[secondEntryIndex] = entries[secondEntryIndex].copy(start = newBorder)
            }
            else -> {
                val entryIndex = getEntryIndexByPointIndex(pointIndex)
                val entry = entries[entryIndex]
                val points = entry.points

                val constraints = labelerConf.connectedConstraints
                val min = if (forcedDrag) leftBorder else {
                    constraints.filter { it.second == pointIndex }
                        .maxOfOrNull { points[it.first] }
                        ?: if (labelerConf.useImplicitStart) leftBorder else entry.start
                }
                val max = if (forcedDrag) rightBorder - 1 else {
                    constraints.filter { it.first == pointIndex }
                        .minOfOrNull { points[it.second] }
                        ?: if (labelerConf.useImplicitEnd) rightBorder - 1 else entry.end
                }
                val newPoints = points.toMutableList()
                val pointInsideIndex = pointIndex % (labelerConf.fields.size + 1)
                newPoints[pointInsideIndex] = x.coerceIn(min, max).runIf(!forcedDrag) {
                    val currentPoints = currentEntries[entryIndex].points
                    snapDrag.update(current = currentPoints[pointInsideIndex], min = min, max = max)
                    snapDrag.snap(this)
                }
                val newEntry = entry.copy(points = newPoints)
                entries[entryIndex] = newEntry
            }
        }
        if (labelerConf.useImplicitStart) {
            entries[0] = entries[0].validateImplicit(labelerConf)
        }
        if (labelerConf.useImplicitEnd) {
            entries[entries.lastIndex] = entries[entries.lastIndex].validateImplicit(labelerConf)
        }
        if (forcedDrag) {
            val lastEntryIndex = entries.lastIndex
            val (leftEntryIndex, currentEntryIndex, rightEntryIndex) = when {
                pointIndex == MarkerCursorState.NONE_POINT_INDEX -> return entries
                pointIndex == MarkerCursorState.START_POINT_INDEX ->
                    Triple(null, null, if (lastEntryIndex >= 0) 0 else null)
                pointIndex == MarkerCursorState.END_POINT_INDEX ->
                    Triple((lastEntryIndex).takeIf { it >= 0 }, null, null)
                isBorderIndex(pointIndex) -> {
                    val (firstEntryIndex, secondEntryIndex) = getEntryIndexesByBorderIndex(pointIndex)
                    Triple(firstEntryIndex, null, secondEntryIndex)
                }
                else -> {
                    val entryIndex = getEntryIndexByPointIndex(pointIndex)
                    Triple(
                        (entryIndex - 1).takeIf { it >= 0 },
                        entryIndex,
                        (entryIndex + 1).takeIf { it <= lastEntryIndex },
                    )
                }
            }
            if (currentEntryIndex != null) {
                val currentEntry = entries[currentEntryIndex]
                val fieldIndex = getFieldIndexByPointIndex(pointIndex)
                val point = currentEntry.points[fieldIndex]
                val start = currentEntry.start.coerceAtMost(point)
                val end = currentEntry.end.coerceAtLeast(point)
                val points = currentEntry.points.toMutableList()
                val constraints = labelerConf.connectedConstraints
                val indexesLeftThanBase = constraints.filter { it.second == fieldIndex }.map { it.first }
                val indexesRightThanBase = constraints.filter { it.first == fieldIndex }.map { it.second }
                points.indices.minus(fieldIndex).forEach { i ->
                    when (i) {
                        in indexesLeftThanBase -> {
                            points[i] = points[i].coerceAtMost(point)
                        }
                        in indexesRightThanBase -> {
                            points[i] = points[i].coerceAtLeast(point)
                        }
                    }
                }
                entries[currentEntryIndex] = currentEntry.copy(
                    start = start,
                    end = end,
                    points = points,
                )
                if (leftEntryIndex != null) {
                    entries[leftEntryIndex] = entries[leftEntryIndex].copy(end = start)
                }
                if (rightEntryIndex != null) {
                    entries[rightEntryIndex] = entries[rightEntryIndex].copy(start = end)
                }
            }
            if (rightEntryIndex != null) {
                var left = entries[rightEntryIndex].start
                for (i in rightEntryIndex until entries.size) {
                    val updated = entries[i].collapsed(leftBorder = left)
                    if (updated == entries[i]) break
                    entries[i] = updated
                    left = entries[i].end
                }
            }
            if (leftEntryIndex != null) {
                var right = entries[leftEntryIndex].end
                for (i in leftEntryIndex downTo 0) {
                    val updated = entries[i].collapsed(rightBorder = right)
                    if (updated == entries[i]) break
                    entries[i] = updated
                    right = entries[i].start
                }
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
        labelShiftUp: Dp,
    ): Int {
        if (isLabelHovered) return MarkerCursorState.NONE_POINT_INDEX

        // end
        if ((endInPixel - x).absoluteValue <= NEAR_RADIUS_START_OR_END) {
            if (x >= endInPixel) return MarkerCursorState.END_POINT_INDEX
            val prev = middlePointsInPixelSorted.lastOrNull() ?: startInPixel
            if (endInPixel - x <= x - prev) return MarkerCursorState.END_POINT_INDEX
        }

        // start
        if ((startInPixel - x).absoluteValue <= NEAR_RADIUS_START_OR_END) {
            if (x <= startInPixel) return MarkerCursorState.START_POINT_INDEX
            val next = middlePointsInPixelSorted.firstOrNull() ?: endInPixel
            if (x - startInPixel <= next - x) return MarkerCursorState.START_POINT_INDEX
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
        (listOf(IndexedValue(MarkerCursorState.START_POINT_INDEX, startInPixel)) + pointsSorted)
            .reversed()
            .zipWithNext()
            .forEach { (current, next) ->
                val radius = if (isBorderIndex(current.index)) NEAR_RADIUS_START_OR_END else NEAR_RADIUS_CUSTOM

                fun inRange(point: IndexedValue<Float>) = (x - point.value).absoluteValue <= radius

                if (!inRange(current)) return@forEach
                if (current.value == next.value ||
                    (x - current.value).absoluteValue <= (next.value - x).absoluteValue
                ) {
                    if (inRange(next) && x < current.value) return@forEach
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

        // check start again
        if ((endInPixel - x).absoluteValue <= NEAR_RADIUS_START_OR_END) {
            return MarkerCursorState.END_POINT_INDEX
        }

        // check end again
        if ((startInPixel - x).absoluteValue <= NEAR_RADIUS_START_OR_END) {
            MarkerCursorState.START_POINT_INDEX
        }

        return MarkerCursorState.NONE_POINT_INDEX
    }

    fun getClickedAudioRange(
        x: Float,
        leftBorder: Float,
        rightBorder: Float,
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

    fun isValidPlaybackPosition(position: Float) = position < entryConverter.convertToPixel(sampleLengthMillis)

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

    fun getUpdatedEntriesByKeyAction(
        action: KeyAction,
        appConf: AppConf,
        labelerConf: LabelerConf,
    ): Pair<List<EntryInPixel>, Int>? {
        val paramIndex = when (action) {
            KeyAction.SetValue1 -> 0
            KeyAction.SetValue2 -> 1
            KeyAction.SetValue3 -> 2
            KeyAction.SetValue4 -> 3
            KeyAction.SetValue5 -> 4
            KeyAction.SetValue6 -> 5
            KeyAction.SetValue7 -> 6
            KeyAction.SetValue8 -> 7
            KeyAction.SetValue9 -> 8
            KeyAction.SetValue10 -> 9
            else -> return null
        }

        // Only used in single edit mode
        if (entries.size != 1) return null

        val fieldCount = this.labelerConf.fields.filter { it.shortcutIndex != null }.size
        val pointIndex = when {
            paramIndex == 0 -> MarkerCursorState.START_POINT_INDEX
            paramIndex == fieldCount + 1 -> MarkerCursorState.END_POINT_INDEX
            paramIndex <= fieldCount -> this.labelerConf.fields.indexOfFirst { it.shortcutIndex == paramIndex }
                .takeIf { it >= 0 } ?: return null
            else -> return null
        }
        val cursorPosition = cursorState.value.position ?: return null
        val lockDrag = appConf.editor.lockedSettingParameterWithCursor &&
            when (appConf.editor.lockedDrag) {
                AppConf.Editor.LockedDrag.UseLabeler -> {
                    val lockedDragByBaseField =
                        labelerConf.lockedDrag.useDragBase &&
                            labelerConf.fields.getOrNull(pointIndex)?.dragBase == true
                    val lockedDragByStart =
                        labelerConf.lockedDrag.useStart && pointIndex == MarkerCursorState.START_POINT_INDEX
                    lockedDragByBaseField || lockedDragByStart
                }
                AppConf.Editor.LockedDrag.UseStart -> pointIndex == MarkerCursorState.START_POINT_INDEX
                else -> false
            }
        val entries = if (lockDrag) {
            getLockedDraggedEntries(pointIndex, cursorPosition, forcedDrag = false)
        } else {
            getDraggedEntries(pointIndex, cursorPosition, forcedDrag = false)
        }
        return entries to pointIndex
    }

    val isCursor: Boolean
        get() = scissorsState.value == null && panState.value == null && playbackState.value == null

    fun switchTool(tool: Tool) {
        Tool.entries.forEach {
            if (it == tool) {
                createToolStateIfNeeded(it)
            } else {
                clearToolState(it)
            }
        }
    }

    private fun createToolStateIfNeeded(tool: Tool) = when (tool) {
        Tool.Cursor -> Unit
        Tool.Scissors -> scissorsState.update { MarkerScissorsState() }
        Tool.Pan -> panState.update { MarkerPanState() }
        Tool.Playback -> playbackState.update { MarkerPlaybackState() }
    }

    private fun clearToolState(tool: Tool) = when (tool) {
        Tool.Cursor -> cursorState.update { MarkerCursorState() }
        Tool.Scissors -> scissorsState.clear()
        Tool.Pan -> panState.clear()
        Tool.Playback -> playbackState.clear()
    }

    companion object {
        private const val NEAR_RADIUS_START_OR_END = 20f
        private const val NEAR_RADIUS_CUSTOM = 5f
    }
}

@Composable
fun rememberMarkerState(
    canvasState: CanvasState.Loaded,
    editorState: EditorState,
    appState: AppState,
): MarkerState {
    val sampleInfo = canvasState.sampleInfo
    val canvasParams = canvasState.params
    val sampleRate = sampleInfo.sampleRate
    val sampleLengthMillis = sampleInfo.lengthMillis
    val entries = editorState.editedEntries
    val project = editorState.project
    val allEntriesInCurrentGroup = remember(entries, project.currentModuleIndex) {
        project.currentModule.getEntriesInGroupForEditing()
    }
    val labelerConf = project.labelerConf
    val entryConverter = remember(sampleInfo.sampleRate, canvasParams.resolution) {
        EntryConverter(sampleInfo.sampleRate, canvasParams.resolution)
    }
    val entriesInPixel = remember(entries, canvasParams, sampleLengthMillis) {
        entries.map { entry ->
            entryConverter.convertToPixel(entry, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val entriesInSampleInPixel = remember(entriesInPixel, canvasParams, sampleLengthMillis) {
        allEntriesInCurrentGroup.map { entry ->
            entryConverter.convertToPixel(entry, sampleLengthMillis).validate(canvasParams.lengthInPixel)
        }
    }
    val leftBorder = remember(entriesInPixel, canvasParams) {
        val previousEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getPreviousOrNull { it.index == entriesInPixel.first().index }
        } else null
        previousEntry?.start ?: 0f
    }
    val rightBorder = remember(entriesInPixel, canvasParams) {
        val nextEntry = if (labelerConf.continuous) {
            entriesInSampleInPixel.getNextOrNull { it.index == entriesInPixel.last().index }
        } else null
        nextEntry?.end ?: canvasParams.lengthInPixel
    }
    val cursorState = remember { mutableStateOf(MarkerCursorState()) }
    val scissorsState = remember { mutableStateOf<MarkerScissorsState?>(null) }
    val panState = remember { mutableStateOf<MarkerPanState?>(null) }
    val playbackState = remember { mutableStateOf<MarkerPlaybackState?>(null) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val waveformsHeightRatio = appState.appConf.painter.amplitudeHeightRatio
    val snapDrag = remember(project, canvasParams) {
        SnapDrag(
            project,
            canvasParams.lengthInPixel,
            entryConverter,
        )
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
        panState,
        canvasHeightState,
        waveformsHeightRatio,
    ) {
        MarkerState(
            entries,
            allEntriesInCurrentGroup,
            labelerConf,
            canvasParams,
            sampleLengthMillis,
            entryConverter,
            entriesInPixel,
            entriesInSampleInPixel,
            leftBorder,
            rightBorder,
            cursorState,
            scissorsState,
            panState,
            playbackState,
            canvasHeightState,
            waveformsHeightRatio,
            snapDrag,
        )
    }
}
