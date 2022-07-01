@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerMouseState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerMouseState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerMouseState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerMouseState.Mouse
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.update
import kotlin.math.abs
import kotlin.math.min

private const val RegionAlpha = 0.3f
private val EditableOutsideRegionColor = White
private const val UneditableRegionAlpha = 0.9f
private val UneditableRegionColor = Black
private const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
private val LabelSize = DpSize(40.dp, 25.dp)
private val LabelShiftUp = 8.dp
private val NameLabelLeftMargin = 5.dp
private val NameLabelTopMargin = 2.dp

@Composable
fun MarkerCanvas(
    sample: Sample,
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    editorState: EditorState,
    appState: AppState,
    state: MarkerState = rememberMarkerState(sample, canvasParams, editorState, appState)
) {

    FieldBorderCanvas(editorState, appState, state)
    FieldLabelCanvas(state)
    if (state.labelerConf.continuous) {
        NameLabelCanvas(state)
    }
    LaunchAdjustScrollPosition(
        state.entryInPixel,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel
    )
}

@Composable
private fun FieldBorderCanvas(
    editorState: EditorState,
    appState: AppState,
    state: MarkerState
) {
    val updateEntry = editorState::updateEntry
    val submitEntry = editorState::submitEntry
    val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()

    Canvas(
        Modifier.fillMaxHeight()
            .width(state.canvasParams.canvasWidthInDp)
            .onPointerEvent(PointerEventType.Move) { event ->
                handleMouseMove(event, updateEntry, state)
            }
            .onPointerEvent(PointerEventType.Press) {
                handleMousePress(keyboardState, state.mouseState, state.labelerConf)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                handleMouseRelease(
                    event,
                    submitEntry,
                    appState.player::playSection,
                    keyboardState,
                    state
                )
            }
    ) {
        try {
            val entryInPixel = state.entryInPixel
            val start = entryInPixel.start
            val end = entryInPixel.end
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val mouseState = state.mouseState
            val labelerConf = state.labelerConf
            state.canvasHeightState.value = canvasHeight

            // Draw left border
            if (leftBorder > 0) {
                val leftBorderColor = UneditableRegionColor
                drawRect(
                    color = leftBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset.Zero,
                    size = Size(width = leftBorder, height = canvasHeight)
                )
                drawLine(
                    color = leftBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(leftBorder, 0f),
                    end = Offset(leftBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw start
            val startColor = EditableOutsideRegionColor
            drawRect(
                color = startColor,
                alpha = RegionAlpha,
                topLeft = Offset(leftBorder, 0f),
                size = Size(width = start - leftBorder, height = canvasHeight)
            )
            val startLineAlpha = if (mouseState.value.usingStartPoint) 1f else IdleLineAlpha
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
                val waveformsHeight = canvasHeight * state.waveformsHeightRatio
                val height = waveformsHeight * field.height
                val top = waveformsHeight - height
                val color = parseColor(field.color)
                val fillTargetIndex = when (field.filling) {
                    "start" -> StartPointIndex
                    "end" -> EndPointIndex
                    null -> null
                    else -> labelerConf.fields.withIndex().find { it.value.name == field.filling }?.index
                }
                if (fillTargetIndex != null) {
                    val targetX = entryInPixel.getPoint(fillTargetIndex)
                    val left = min(targetX, x)
                    val width = abs(targetX - x)
                    drawRect(
                        color = color,
                        alpha = RegionAlpha,
                        topLeft = Offset(left, top),
                        size = Size(width = width, height = height)
                    )
                }
                val lineAlpha = if (mouseState.value.pointIndex != i) IdleLineAlpha else 1f
                drawLine(
                    color = color.copy(alpha = lineAlpha),
                    start = Offset(x, top),
                    end = Offset(x, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw end
            val endColor = EditableOutsideRegionColor
            drawRect(
                color = endColor,
                alpha = RegionAlpha,
                topLeft = Offset(end, 0f),
                size = Size(width = rightBorder - end, height = canvasHeight)
            )
            val endLineAlpha = if (mouseState.value.usingEndPoint) 1f else IdleLineAlpha
            drawLine(
                color = endColor.copy(alpha = endLineAlpha),
                start = Offset(end, 0f),
                end = Offset(end, canvasHeight),
                strokeWidth = StrokeWidth
            )

            // Draw right border
            if (rightBorder < canvasWidth) {
                val rightBorderColor = UneditableRegionColor
                drawRect(
                    color = rightBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset(rightBorder, 0f),
                    size = Size(width = canvasWidth - rightBorder, height = canvasHeight)
                )
                drawLine(
                    color = rightBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(rightBorder, 0f),
                    end = Offset(rightBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }
        } catch (t: Throwable) {
            Log.debug(t)
        }
    }
}

@Composable
private fun FieldLabelCanvas(state: MarkerState) = FieldLabelCanvasLayout(
    modifier = Modifier.fillMaxHeight().width(state.canvasParams.canvasWidthInDp),
    waveformsHeightRatio = state.waveformsHeightRatio,
    fields = state.labelerConf.fields,
    entry = state.entryInPixel
) {
    for (i in state.labelerConf.fields.indices) {
        val field = state.labelerConf.fields[i]
        val alpha = if (state.mouseState.value.pointIndex != i) IdleLineAlpha else 1f
        Box(
            modifier = Modifier.requiredSize(LabelSize),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = field.label,
                textAlign = TextAlign.Center,
                color = parseColor(field.color).copy(alpha = alpha),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.caption.copy(fontSize = 14.sp)
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
    val labelShiftUp = with(LocalDensity.current) { LabelShiftUp.toPx() }
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
                placeable.place(x.toInt(), y.toInt())
            }
        }
    }
}

@Composable
private fun NameLabelCanvas(state: MarkerState) {
    val leftName = state.entriesInSample.getOrNull(state.currentIndexInSample - 1)?.name
    val rightName = state.entriesInSample.getOrNull(state.currentIndexInSample + 1)?.name
    NameLabelCanvasLayout(
        modifier = Modifier.fillMaxHeight().width(state.canvasParams.canvasWidthInDp),
        entry = state.entryInPixel,
        leftBorder = state.leftBorder.takeIf { leftName != null },
        rightBorder = state.rightBorder.takeIf { rightName != null }
    ) {
        if (leftName != null) {
            NameLabel(leftName, Black)
        }
        NameLabel(state.entryInPixel.name, DarkYellow)
        if (rightName != null) {
            NameLabel(rightName, Black)
        }
    }
}

@Composable
private fun NameLabel(name: String, color: Color) {
    Text(
        modifier = Modifier.widthIn(max = 100.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        text = name,
        color = color,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun NameLabelCanvasLayout(
    modifier: Modifier,
    entry: EntryInPixel,
    leftBorder: Float?,
    rightBorder: Float?,
    content: @Composable () -> Unit
) {
    val labelTopMargin = with(LocalDensity.current) { NameLabelTopMargin.toPx() }
    val labelLeftMargin = with(LocalDensity.current) { NameLabelLeftMargin.toPx() }
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = labelTopMargin.toInt()
            val xs = listOfNotNull(
                leftBorder,
                entry.start,
                rightBorder?.let { entry.end }
            )
            placeables.forEachIndexed { index, placeable ->
                val x = xs[index] + labelLeftMargin
                placeable.place(x.toInt(), y)
            }
        }
    }
}

private fun handleMouseMove(
    event: PointerEvent,
    editEntry: (Entry) -> Unit,
    state: MarkerState
) {
    val eventChange = event.changes.first()
    val entryInPixel = state.entryInPixel
    val leftBorder = state.leftBorder
    val rightBorder = state.rightBorder
    val mouseState = state.mouseState
    val labelerConf = state.labelerConf
    val x = eventChange.position.x.coerceIn(0f, state.canvasParams.lengthInPixel.toFloat())
    val y = eventChange.position.y.coerceIn(0f, state.canvasHeightState.value.coerceAtLeast(0f))
    if (mouseState.value.mouse == Mouse.Dragging) {
        val newEntryInPixel = if (mouseState.value.lockedDrag) {
            entryInPixel.lockedDrag(mouseState.value.pointIndex, x, leftBorder, rightBorder)
        } else {
            entryInPixel.drag(mouseState.value.pointIndex, x, leftBorder, rightBorder, labelerConf)
        }
        if (newEntryInPixel != entryInPixel) {
            editEntry(state.entryConverter.convertToMillis(newEntryInPixel))
        }
    } else {
        val newPointIndex = entryInPixel.getPointIndexForHovering(
            x = x,
            y = y,
            conf = labelerConf,
            canvasHeight = state.canvasHeightState.value,
            waveformsHeightRatio = state.waveformsHeightRatio,
            density = state.canvasParams.density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp
        )
        if (newPointIndex == NonePointIndex) {
            mouseState.update { moveToNothing() }
        } else {
            mouseState.update { moveToHover(newPointIndex) }
        }
    }
}

private fun handleMousePress(
    keyboardState: KeyboardState,
    state: MutableState<MarkerMouseState>,
    labelerConf: LabelerConf
) {
    if (!keyboardState.isCtrlPressed) {
        if (state.value.mouse == Mouse.Hovering) {
            val lockedDragByBaseField =
                labelerConf.lockedDrag.useDragBase &&
                    labelerConf.fields.getOrNull(state.value.pointIndex)?.dragBase == true
            val lockedDragByStart =
                labelerConf.lockedDrag.useStart && state.value.usingStartPoint
            val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor keyboardState.isShiftPressed
            state.update { startDragging(lockedDrag) }
        }
    }
}

private fun handleMouseRelease(
    event: PointerEvent,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    keyboardState: KeyboardState,
    state: MarkerState
) {
    val mouseState = state.mouseState
    val entryConverter = state.entryConverter
    if (keyboardState.isCtrlPressed) {
        val x = event.changes.first().position.x
        val clickedRange = state.entryInPixel.getClickedAudioRange(x, state.leftBorder, state.rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: state.canvasParams.dataLength.toFloat()
            playSampleSection(start, end)
        }
    } else {
        submitEntry()
        mouseState.update { finishDragging() }
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    entryInPixel: EntryInPixel,
    canvasLength: Int,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel
) {
    LaunchedEffect(entryInPixel.name, canvasLength, horizontalScrollState.maxValue) {
        val scrollMax = horizontalScrollState.maxValue
        val screenLength = canvasLength.toFloat() - scrollMax
        val start = entryInPixel.start
        val end = entryInPixel.end
        val center = (start + end) / 2
        val target = (center - screenLength / 2).toInt().coerceAtMost(scrollMax).coerceAtLeast(0)
        scrollFitViewModel.update(target)
    }
}
