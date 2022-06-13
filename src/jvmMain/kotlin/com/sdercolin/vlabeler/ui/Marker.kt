@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.MarkerState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.MarkerState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.MarkerState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.MarkerState.MouseState
import com.sdercolin.vlabeler.ui.model.CanvasParams
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.toFrame
import com.sdercolin.vlabeler.util.toMillisecond
import com.sdercolin.vlabeler.util.update
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

private data class MarkerState(
    val mouse: MouseState = MouseState.None,
    val pointIndex: Int = -3, // -3: null, -2: start, -1: end
    val lockedDrag: Boolean = false
) {

    enum class MouseState {
        Dragging,
        Hovering,
        None
    }

    val usingStartPoint get() = pointIndex == StartPointIndex
    val usingEndPoint get() = pointIndex == EndPointIndex

    companion object {
        const val NonePointIndex = -3
        const val StartPointIndex = -2
        const val EndPointIndex = -1
    }
}

private data class EntryInPixel(
    val name: String,
    val start: Float,
    val end: Float,
    val points: List<Float>
) {

    private val pointsSorted = points.sorted()

    fun getPoint(index: Int): Float = when (index) {
        StartPointIndex -> start
        EndPointIndex -> end
        else -> getCustomPoint(index)
    }

    fun getCustomPoint(index: Int): Float = points.getOrNull(index) ?: points.lastOrNull() ?: start

    fun getPointIndexForHovering(x: Float, y: Float, conf: LabelerConf, canvasHeight: Float, density: Density): Int {
        // end
        if ((end - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x >= end) return EndPointIndex
            val prev = pointsSorted.lastOrNull() ?: start
            if (end - x <= x - prev) return EndPointIndex
        }

        // start
        if ((start - x).absoluteValue <= NearRadiusStartOrEnd) {
            if (x <= start) return StartPointIndex
            val next = pointsSorted.firstOrNull() ?: end
            if (x - start <= next - x) return StartPointIndex
        }

        // other points
        val pointsSorted = points.withIndex().sortedBy { it.value }
        for ((index, value) in pointsSorted) {
            // label part
            val centerY = canvasHeight * (1 - conf.fields[index].height) - with(density) { labelShiftUp.toPx() }
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
        for ((current, next) in (pointsSorted + listOf(IndexedValue(EndPointIndex, end))).zipWithNext()) {
            // line part
            if ((current.value - x).absoluteValue > NearRadiusCustom) continue
            if (current.value == next.value || x - current.value <= next.value - x) {
                val top = canvasHeight * (1 - conf.fields[current.index].height)
                if (y >= top) return current.index
            }
        }

        return NonePointIndex
    }

    fun drag(pointIndex: Int, x: Float, conf: LabelerConf, canvasWidthInPixel: Int): EntryInPixel =
        when (pointIndex) {
            NonePointIndex -> this
            StartPointIndex -> {
                val max = pointsSorted.firstOrNull() ?: end
                copy(start = x.coerceIn(0f, max))
            }
            EndPointIndex -> {
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
        if (pointIndex == NonePointIndex) return this
        val dxMin = -start
        val dxMax = canvasWidthInPixel.toFloat() - 1 - end
        val dx = (x - getPoint(pointIndex)).coerceIn(dxMin, dxMax)
        return copy(start = start + dx, end = end + dx, points = points.map { it + dx })
    }

    fun getClickedRange(x: Float): Pair<Float?, Float?>? {
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

private class Converter(
    val sampleRate: Float,
    val resolution: Int
) {
    fun convertToPixel(entry: Entry) = EntryInPixel(
        name = entry.name,
        start = convertToPixel(entry.start),
        end = convertToPixel(entry.end),
        points = entry.points.map { convertToPixel(it) }
    )

    private fun convertToPixel(millis: Float) = toFrame(millis, sampleRate).div(resolution)

    fun convertToMillis(entry: EntryInPixel) = Entry(
        name = entry.name,
        start = convertToMillis(entry.start),
        end = convertToMillis(entry.end),
        points = entry.points.map { convertToMillis(it) }
    )

    private fun convertToMillis(px: Float) = toMillisecond(convertToFrame(px), sampleRate)
    fun convertToFrame(px: Float) = px.times(resolution)
}

private const val RegionAlpha = 0.1f
private const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
private val labelSize = DpSize(40.dp, 25.dp)
private val labelShiftUp = 8.dp

@Composable
fun MarkerCanvas(
    canvasParams: CanvasParams,
    labelerConf: LabelerConf,
    keyboardState: KeyboardState,
    sampleRate: Float,
    entry: Entry,
    editEntry: (Entry) -> Unit,
    playSampleSection: (Float, Float) -> Unit
) {
    val converter = Converter(sampleRate, canvasParams.resolution)
    val entryInPixel = converter.convertToPixel(entry)
    val state = remember { mutableStateOf(MarkerState()) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val fields = labelerConf.fields
    val primaryColor = MaterialTheme.colors.primary

    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidthInDp)
            .onPointerEvent(PointerEventType.Move) { event ->
                val eventChange = event.changes.first()
                val x = eventChange.position.x.coerceIn(0f, canvasParams.lengthInPixel - 1f)
                val y = eventChange.position.y.coerceIn(0f, (canvasHeightState.value - 1f).coerceAtLeast(0f))
                if (state.value.mouse == MouseState.Dragging) {
                    val newEntryInPixel = if (state.value.lockedDrag) {
                        entryInPixel.lockedDrag(state.value.pointIndex, x, canvasParams.lengthInPixel)
                    } else {
                        entryInPixel.drag(state.value.pointIndex, x, labelerConf, canvasParams.lengthInPixel)
                    }
                    if (newEntryInPixel != entryInPixel) {
                        editEntry(converter.convertToMillis(newEntryInPixel))
                    }
                } else {
                    val newPointIndex = entryInPixel
                        .getPointIndexForHovering(x, y, labelerConf, canvasHeightState.value, canvasParams.density)
                    if (newPointIndex == NonePointIndex) {
                        state.update {
                            copy(pointIndex = newPointIndex, mouse = MouseState.None)
                        }
                    } else {
                        state.update {
                            copy(pointIndex = newPointIndex, mouse = MouseState.Hovering)
                        }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Press) {
                if (!keyboardState.isCtrlPressed) {
                    if (state.value.mouse == MouseState.Hovering) {
                        val lockedDragByBaseField =
                            labelerConf.lockedDrag.useDragBase &&
                                    labelerConf.fields.getOrNull(state.value.pointIndex)?.dragBase == true
                        val lockedDragByStart =
                            labelerConf.lockedDrag.useStart && state.value.usingStartPoint
                        val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor keyboardState.isShiftPressed
                        state.update { copy(mouse = MouseState.Dragging, lockedDrag = lockedDrag) }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                if (keyboardState.isCtrlPressed) {
                    val x = event.changes.first().position.x
                    val clickedRange = entryInPixel.getClickedRange(x)
                    if (clickedRange != null) {
                        val start = clickedRange.first?.let { converter.convertToFrame(it) } ?: 0f
                        val end = clickedRange.second?.let { converter.convertToFrame(it) }
                            ?: canvasParams.dataLength.toFloat()
                        playSampleSection(start, end)
                    }
                } else {
                    state.update { copy(mouse = MouseState.None, lockedDrag = false) }
                }
            }
    ) {
        val start = entryInPixel.start
        val end = entryInPixel.end
        val canvasWidth = size.width
        val canvasHeight = size.height
        canvasHeightState.value = canvasHeight

        // Draw start
        val startColor = primaryColor
        drawRect(
            color = startColor,
            alpha = RegionAlpha,
            topLeft = Offset.Zero,
            size = Size(width = start, height = canvasHeight)
        )
        val startLineAlpha = if (state.value.usingStartPoint) IdleLineAlpha else 1f
        drawLine(
            color = startColor.copy(alpha = startLineAlpha),
            start = Offset(start, 0f),
            end = Offset(start, canvasHeight),
            strokeWidth = StrokeWidth
        )

        // Draw custom fields
        for (i in fields.indices) {
            val field = fields[i]
            val x = entryInPixel.getCustomPoint(i)
            val height = canvasHeight * field.height
            val top = canvasHeight - height
            val color = parseColor(field.color)
            if (field.filling != null) {
                val targetX = entryInPixel.getPoint(field.filling)
                val left = min(targetX, x)
                val width = abs(targetX - x)
                drawRect(
                    color = color,
                    alpha = RegionAlpha,
                    topLeft = Offset(left, top),
                    size = Size(width = width, height = height)
                )
            }
            val lineAlpha = if (state.value.pointIndex != i) IdleLineAlpha else 1f
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = Offset(x, top),
                end = Offset(x, canvasHeight),
                strokeWidth = StrokeWidth
            )
        }

        // Draw end
        val endColor = primaryColor
        drawRect(
            color = endColor,
            alpha = RegionAlpha,
            topLeft = Offset(end, 0f),
            size = Size(width = canvasWidth - end, height = canvasHeight)
        )
        val endLineAlpha = if (state.value.usingEndPoint) 1f else IdleLineAlpha
        drawLine(
            color = endColor.copy(alpha = endLineAlpha),
            start = Offset(end, 0f),
            end = Offset(end, canvasHeight),
            strokeWidth = StrokeWidth
        )
    }
    FieldLabelCanvas(canvasParams, state.value, labelerConf, entryInPixel)
}

@Composable
private fun FieldLabelCanvas(
    canvasParams: CanvasParams,
    state: MarkerState,
    conf: LabelerConf,
    entryInPixel: EntryInPixel
) {
    FieldLabelCanvasLayout(Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp), conf.fields, entryInPixel) {
        for (i in conf.fields.indices) {
            val field = conf.fields[i]
            val alpha = if (state.pointIndex != i) IdleLineAlpha else 1f
            Box(
                modifier = Modifier.requiredSize(labelSize),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = field.abbr,
                    textAlign = TextAlign.Center,
                    color = parseColor(field.color).copy(alpha = alpha),
                    style = MaterialTheme.typography.body2.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun FieldLabelCanvasLayout(
    modifier: Modifier,
    fields: List<LabelerConf.Field>,
    entry: EntryInPixel,
    content: @Composable () -> Unit
) {
    val labelShiftUp = with(LocalDensity.current) { labelShiftUp.toPx() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val field = fields[index]
                val x = entry.getCustomPoint(index) - (constraints.maxWidth) / 2
                val y =
                    constraints.maxHeight.toFloat() * (1 - field.height) - (constraints.maxHeight) / 2 - labelShiftUp
                placeable.place(x = x.toInt(), y = y.toInt())
            }
        }
    }
}