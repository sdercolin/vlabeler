@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import org.jetbrains.skia.Font

private data class MarkerState(
    val mouse: MouseState = MouseState.None,
    val pointIndex: Int = -3 // -3: null, -2: start, -1: end
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

    fun getPoint(index: Int): Float {
        return points.getOrNull(index) ?: points.lastOrNull() ?: start
    }

    fun getPointIndex(x: Float): Int {
        // end
        if ((end - x).absoluteValue <= NearRadius) {
            if (x >= end) return EndPointIndex
            val prev = pointsSorted.lastOrNull() ?: start
            if (end - x <= x - prev) return EndPointIndex
        }

        // start
        if ((start - x).absoluteValue <= NearRadius) {
            if (x <= start) return StartPointIndex
            val next = pointsSorted.firstOrNull() ?: end
            if (x - start <= next - x) return StartPointIndex
        }

        val pointsSorted = points.withIndex().sortedBy { it.value }
        for ((current, next) in (pointsSorted + listOf(IndexedValue(EndPointIndex, end))).zipWithNext()) {
            if ((current.value - x).absoluteValue > NearRadius) continue
            if (current.value == next.value) return current.index
            if (x - current.value <= next.value - x) return current.index
        }

        return NonePointIndex
    }

    fun drag(pointIndex: Int, point: Float): EntryInPixel =
        when (pointIndex) {
            NonePointIndex -> this
            StartPointIndex -> {
                val max = pointsSorted.firstOrNull() ?: end
                copy(start = point.coerceAtMost(max))
            }
            EndPointIndex -> {
                val min = pointsSorted.lastOrNull() ?: start
                copy(end = point.coerceAtLeast(min))
            }
            else -> {
                // TODO: other points
                this
            }
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
        private const val NearRadius = 20f
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
    val primaryColor = MaterialTheme.colors.primary
    Canvas(
        Modifier.fillMaxHeight()
            .width(canvasParams.canvasWidth)
            .onPointerEvent(PointerEventType.Move) {
                val x = it.changes.first().position.x.coerceIn(0f, canvasParams.lengthInPixel - 1f)
                if (state.value.mouse == MouseState.Dragging) {
                    val newEntryInPixel = entryInPixel.drag(state.value.pointIndex, x)
                    if (newEntryInPixel != entryInPixel) {
                        editEntry(converter.convertToMillis(newEntryInPixel))
                    }
                } else {
                    val newPointIndex = entryInPixel.getPointIndex(x)
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
                        state.update { copy(mouse = MouseState.Dragging) }
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
                    state.update { copy(mouse = MouseState.None) }
                }
            }
    ) {
        val regionAlpha = 0.1f
        val idleLineAlpha = 0.5f
        val hoverLineAlpha = 0.8f
        val strokeWidth = 2f
        val start = entryInPixel.start
        val end = entryInPixel.end
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw start
        val startColor = primaryColor
        drawRect(
            color = startColor,
            alpha = regionAlpha,
            topLeft = Offset.Zero,
            size = Size(width = start, height = canvasHeight)
        )
        if (state.value.usingStartPoint &&
            (state.value.mouse == MouseState.Hovering || state.value.mouse == MouseState.Dragging)
        ) {
            val color = if (state.value.mouse == MouseState.Hovering) {
                startColor.copy(alpha = hoverLineAlpha)
            } else {
                startColor
            }
            drawLine(
                color = color,
                start = Offset(start, 0f),
                end = Offset(start, canvasHeight),
                strokeWidth = strokeWidth
            )
        }

        // Draw custom fields
        val fields = labelerConf.fields.sortedBy { it.index }
        for (i in fields.indices) {
            val field = fields[i]
            val x = entryInPixel.getPoint(i)
            println("i=$i, x=$x")
            val height = canvasHeight * field.height
            val top = canvasHeight - height
            val color = parseColor(field.color)
            if (field.filling != null) {
                val targetX = when (field.filling) {
                    StartPointIndex -> start
                    EndPointIndex -> end
                    else -> entryInPixel.getPoint(field.filling)
                }
                val left = min(targetX, x)
                val width = abs(targetX - x)
                drawRect(
                    color = color,
                    alpha = regionAlpha,
                    topLeft = Offset(left, top),
                    size = Size(width = width, height = height)
                )
            }
            val lineAlpha = when {
                state.value.pointIndex != i -> idleLineAlpha
                state.value.mouse == MouseState.Hovering -> hoverLineAlpha
                else -> 1f
            }
            drawLine(
                color = color.copy(alpha = lineAlpha),
                start = Offset(x, top),
                end = Offset(x, canvasHeight),
                strokeWidth = strokeWidth
            )
            drawIntoCanvas {
                it.nativeCanvas.drawString(
                    field.abbr,
                    x = x - 20f,
                    y = top - 10f,
                    font = Font().apply { size = 28f },
                    paint = Paint().asFrameworkPaint().apply { this.color = color.toArgb() }
                )
            }
        }

        // Draw end
        val endColor = primaryColor
        drawRect(
            color = endColor,
            alpha = regionAlpha,
            topLeft = Offset(end, 0f),
            size = Size(width = canvasWidth - end, height = canvasHeight)
        )
        if (state.value.usingEndPoint) {
            val color = if (state.value.mouse == MouseState.Hovering) {
                endColor.copy(alpha = hoverLineAlpha)
            } else {
                endColor
            }
            drawLine(
                color = color,
                start = Offset(end, 0f),
                end = Offset(end, canvasHeight),
                strokeWidth = strokeWidth
            )
        }
    }
}