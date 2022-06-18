@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.labeler.marker.MarkerState.MouseState
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.update
import kotlin.math.abs
import kotlin.math.min

@Immutable
data class MarkerState(
    val mouse: MouseState = MouseState.None,
    val pointIndex: Int = NonePointIndex, // starts from 0 for custom points
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

private const val RegionAlpha = 0.3f
private const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
private val labelSize = DpSize(40.dp, 25.dp)
private val labelShiftUp = 8.dp

@Composable
fun MarkerCanvas(
    entry: Entry,
    sampleLengthMillis: Float,
    editEntry: (Entry) -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    appConf: AppConf,
    labelerConf: LabelerConf,
    canvasParams: CanvasParams,
    sampleRate: Float,
    keyboardState: KeyboardState
) {
    val entryConverter = EntryConverter(sampleRate, canvasParams.resolution)
    val entryInPixel = entryConverter.convertToPixel(entry, sampleLengthMillis)
    val state = remember { mutableStateOf(MarkerState()) }
    val canvasHeightState = remember { mutableStateOf(0f) }
    val waveformsHeightRatio = remember(appConf.painter.spectrogram) {
        val spectrogram = appConf.painter.spectrogram
        val totalWeight = 1f + if (spectrogram.enabled) spectrogram.heightWeight else 0f
        1f / totalWeight
    }
    FieldBorderCanvas(
        canvasParams,
        waveformsHeightRatio,
        canvasHeightState,
        state,
        entryInPixel,
        labelerConf,
        editEntry,
        entryConverter,
        keyboardState,
        playSampleSection
    )
    FieldLabelCanvas(canvasParams, waveformsHeightRatio, state.value, labelerConf, entryInPixel)
}

@Composable
private fun FieldBorderCanvas(
    canvasParams: CanvasParams,
    waveformsHeightRatio: Float,
    canvasHeightState: MutableState<Float>,
    state: MutableState<MarkerState>,
    entryInPixel: EntryInPixel,
    labelerConf: LabelerConf,
    editEntry: (Entry) -> Unit,
    entryConverter: EntryConverter,
    keyboardState: KeyboardState,
    playSampleSection: (Float, Float) -> Unit
) {

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
                        editEntry(entryConverter.convertToMillis(newEntryInPixel))
                    }
                } else {
                    val newPointIndex = entryInPixel.getPointIndexForHovering(
                        x = x,
                        y = y,
                        conf = labelerConf,
                        canvasHeight = canvasHeightState.value,
                        waveformsHeightRatio = waveformsHeightRatio,
                        density = canvasParams.density,
                        labelSize = labelSize,
                        labelShiftUp = labelShiftUp
                    )
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
                    val clickedRange = entryInPixel.getClickedAudioRange(x)
                    if (clickedRange != null) {
                        val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
                        val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
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
        val startColor = White
        drawRect(
            color = startColor,
            alpha = RegionAlpha,
            topLeft = Offset.Zero,
            size = Size(width = start, height = canvasHeight)
        )
        val startLineAlpha = if (state.value.usingStartPoint) 1f else IdleLineAlpha
        drawLine(
            color = startColor.copy(alpha = startLineAlpha),
            start = Offset(start, 0f),
            end = Offset(start, canvasHeight),
            strokeWidth = StrokeWidth
        )

        // Draw custom fields
        for (i in labelerConf.fields.indices) {
            val field = labelerConf.fields[i]
            val x = entryInPixel.getCustomPoint(i)
            val waveformsHeight = canvasHeight * waveformsHeightRatio
            val height = waveformsHeight * field.height
            val top = waveformsHeight - height
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
        val endColor = White
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
}

@Composable
private fun FieldLabelCanvas(
    canvasParams: CanvasParams,
    waveformsHeightRatio: Float,
    state: MarkerState,
    conf: LabelerConf,
    entryInPixel: EntryInPixel
) = FieldLabelCanvasLayout(
    modifier = Modifier.fillMaxHeight().width(canvasParams.canvasWidthInDp),
    waveformsHeightRatio = waveformsHeightRatio,
    fields = conf.fields,
    entry = entryInPixel
) {
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

@Composable
private fun FieldLabelCanvasLayout(
    modifier: Modifier,
    waveformsHeightRatio: Float,
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
                val canvasHeight = constraints.maxHeight.toFloat()
                val waveformsHeight = canvasHeight * waveformsHeightRatio
                val restCanvasHeight = canvasHeight - waveformsHeight
                val height = waveformsHeight * field.height + restCanvasHeight
                val y = canvasHeight - height - labelShiftUp - canvasHeight / 2
                placeable.place(x = x.toInt(), y = y.toInt())
            }
        }
    }
}
