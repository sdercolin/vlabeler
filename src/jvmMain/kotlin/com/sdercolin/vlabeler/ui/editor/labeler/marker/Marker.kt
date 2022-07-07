@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
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
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Sample
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.EndPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.NonePointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.StartPointIndex
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Mouse
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.clear
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.parseColor
import com.sdercolin.vlabeler.util.requireValue
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
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

@Composable
fun MarkerCanvas(
    sample: Sample,
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    editorState: EditorState,
    appState: AppState,
    state: MarkerState = rememberMarkerState(sample, canvasParams, editorState, appState)
) {
    val requestRename: (Int) -> Unit = remember(appState) {
        { appState.openEditEntryNameDialog(it, InputEntryNameDialogPurpose.Rename) }
    }
    FieldBorderCanvas(editorState, appState, state)
    FieldLabelCanvas(state)
    if (state.labelerConf.continuous) {
        NameLabelCanvas(
            state = state,
            requestRename = requestRename
        )
    }
    LaunchAdjustScrollPosition(
        state.entriesInPixel,
        editorState.project.currentIndex,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel
    )
    LaunchedEffect(editorState.tool) {
        if (editorState.tool == Tool.Scissors) {
            state.scissorsState.update { MarkerScissorsState() }
            state.cursorState.update { MarkerCursorState() }
        } else {
            state.scissorsState.clear()
        }
    }
}

@Composable
private fun FieldBorderCanvas(
    editorState: EditorState,
    appState: AppState,
    state: MarkerState
) {
    val keyboardState by appState.keyboardViewModel.keyboardStateFlow.collectAsState()
    val tool = editorState.tool

    LaunchedEffect(keyboardState.isCtrlPressed, tool) {
        if (tool == Tool.Scissors) {
            state.scissorsState.updateNonNull { copy(disabled = keyboardState.isCtrlPressed) }
        }
    }

    Canvas(
        Modifier.fillMaxHeight()
            .width(state.canvasParams.canvasWidthInDp)
            .onPointerEvent(PointerEventType.Move) { event ->
                if (editorState.tool == Tool.Cursor &&
                    state.cursorState.value.mouse == Mouse.Dragging &&
                    event.buttons.areAnyPressed.not()
                ) {
                    state.handleMouseRelease(
                        tool,
                        event,
                        editorState::submitEntries,
                        appState.player::playSection,
                        editorState::cutEntry,
                        keyboardState
                    )
                    Log.info("Handled release in PointerEventType.Move type")
                    return@onPointerEvent
                }
                state.handleMouseMove(tool, event, editorState::updateEntries)
            }
            .onPointerEvent(PointerEventType.Press) {
                state.cursorState.handleMousePress(tool, keyboardState, state.labelerConf)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                state.handleMouseRelease(
                    tool,
                    event,
                    editorState::submitEntries,
                    appState.player::playSection,
                    editorState::cutEntry,
                    keyboardState
                )
            }
    ) {
        try {
            val entriesInPixel = state.entriesInPixel
            val start = entriesInPixel.first().start
            val end = entriesInPixel.last().end
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val cursorState = state.cursorState
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
            val startLineAlpha = if (cursorState.value.usingStartPoint) 1f else IdleLineAlpha
            drawLine(
                color = startColor.copy(alpha = startLineAlpha),
                start = Offset(start, 0f),
                end = Offset(start, canvasHeight),
                strokeWidth = StrokeWidth
            )

            // Draw custom fields and borders
            for (entryIndex in entriesInPixel.indices) {
                val entryInPixel = entriesInPixel[entryIndex]
                if (entryIndex != 0) {
                    val border = state.entryBorders[entryIndex - 1]
                    val borderColor = EditableOutsideRegionColor
                    val pointIndex = cursorState.value.pointIndex
                    val borderLineAlpha =
                        if (state.isBorderIndex(pointIndex) &&
                            state.getEntryIndexesByBorderIndex(pointIndex).second == entryIndex
                        ) 1f else IdleLineAlpha
                    drawLine(
                        color = borderColor.copy(alpha = borderLineAlpha),
                        start = Offset(border, 0f),
                        end = Offset(border, canvasHeight),
                        strokeWidth = StrokeWidth
                    )
                }
                for (fieldIndex in labelerConf.fields.indices) {
                    val field = labelerConf.fields[fieldIndex]
                    val x = entryInPixel.points[fieldIndex]
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
                    val lineAlpha = if (cursorState.value.pointIndex != fieldIndex) IdleLineAlpha else 1f
                    drawLine(
                        color = color.copy(alpha = lineAlpha),
                        start = Offset(x, top),
                        end = Offset(x, canvasHeight),
                        strokeWidth = StrokeWidth
                    )
                }
            }

            // Draw end
            val endColor = EditableOutsideRegionColor
            drawRect(
                color = endColor,
                alpha = RegionAlpha,
                topLeft = Offset(end, 0f),
                size = Size(width = rightBorder - end, height = canvasHeight)
            )
            val endLineAlpha = if (cursorState.value.usingEndPoint) 1f else IdleLineAlpha
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

            // Draw scissors
            state.scissorsState.value?.let { scissors ->
                val position = scissors.position
                if (scissors.disabled.not() && position != null && state.isValidCutPosition(position)) {
                    drawLine(
                        color = parseColor(appState.appConf.editor.scissorsColor),
                        start = Offset(position, 0f),
                        end = Offset(position, canvasHeight),
                        strokeWidth = StrokeWidth * 2
                    )
                }
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
    entries = state.entriesInPixel
) {
    repeat(state.entriesInPixel.size) {
        state.labelerConf.fields.indices.forEach { i ->
            val field = state.labelerConf.fields[i]
            val alpha = if (state.cursorState.value.pointIndex != i) IdleLineAlpha else 1f
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
}

@Composable
private fun FieldLabelCanvasLayout(
    modifier: Modifier,
    waveformsHeightRatio: Float,
    fields: List<LabelerConf.Field>,
    entries: List<EntryInPixel>,
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
                val field = fields[index % fields.size]
                val entry = entries[index / fields.size]
                val x = entry.points[index] - (constraints.maxWidth) / 2
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
private fun NameLabelCanvas(state: MarkerState, requestRename: (Int) -> Unit) {
    val entryIndexes = state.entries.map { it.index }
    val entryNames = state.entries.map { it.name }
    val leftEntry = remember(entryIndexes, entryNames) {
        state.entriesInSample.getPreviousOrNull(state.entries.first())
    }
    val rightEntry = remember(entryIndexes, entryNames) {
        state.entriesInSample.getNextOrNull(state.entries.last())
    }
    NameLabelCanvasLayout(
        modifier = Modifier.fillMaxHeight().width(state.canvasParams.canvasWidthInDp),
        entries = state.entriesInPixel,
        leftBorder = state.leftBorder.takeIf { leftEntry != null },
        rightBorder = state.rightBorder.takeIf { rightEntry != null }
    ) {
        if (leftEntry != null) {
            NameLabel(leftEntry.index, leftEntry.name, Black, requestRename)
        }
        entryIndexes.zip(entryNames).forEach { (index, name) ->
            NameLabel(index, name, DarkYellow, requestRename)
        }
        if (rightEntry != null) {
            NameLabel(rightEntry.index, rightEntry.name, Black, requestRename)
        }
    }
}

@Composable
private fun NameLabel(index: Int, name: String, color: Color, requestRename: (Int) -> Unit) {
    Log.info("NameLabel($name) composed")
    Text(
        modifier = Modifier.widthIn(max = 100.dp)
            .wrapContentSize()
            .clickable { requestRename(index) }
            .padding(vertical = 2.dp, horizontal = 5.dp),
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
    entries: List<EntryInPixel>,
    leftBorder: Float?,
    rightBorder: Float?,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            val xs = listOf(
                listOfNotNull(leftBorder),
                entries.map { it.start },
                listOfNotNull(rightBorder)
            ).flatten()
            placeables.forEachIndexed { index, placeable ->
                val x = xs[index]
                placeable.place(x.toInt(), 0)
            }
        }
    }
}

private fun MarkerState.handleMouseMove(
    tool: Tool,
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit
) {
    when (tool) {
        Tool.Cursor -> handleCursorMove(event, editEntries)
        Tool.Scissors -> handleScissorsMove(event)
    }
}

private fun MarkerState.handleCursorMove(
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x.coerceIn(0f, canvasParams.lengthInPixel.toFloat())
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (cursorState.value.mouse == Mouse.Dragging) {
        val updated = if (cursorState.value.lockedDrag) {
            getLockedDraggedEntries(cursorState.value.pointIndex, x, leftBorder, rightBorder)
        } else {
            getDraggedEntries(cursorState.value.pointIndex, x, leftBorder, rightBorder, labelerConf)
        }
        if (updated != entriesInPixel) {
            val updatedInMillis = updated.map { entryConverter.convertToMillis(it) }
            editEntries(updatedInMillis)
        }
    } else {
        val newPointIndex = getPointIndexForHovering(
            x = x,
            y = y,
            conf = labelerConf,
            canvasHeight = canvasHeightState.value,
            waveformsHeightRatio = waveformsHeightRatio,
            density = canvasParams.density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp
        )
        if (newPointIndex == NonePointIndex) {
            cursorState.update { moveToNothing() }
        } else {
            cursorState.update { moveToHover(newPointIndex) }
        }
    }
}

private fun MarkerState.handleScissorsMove(
    event: PointerEvent
) {
    val scissorsState = scissorsState
    val x = event.changes.first().position.x
    val position = x.takeIf { isValidCutPosition(it) }
    scissorsState.updateNonNull { copy(position = position) }
}

private fun MutableState<MarkerCursorState>.handleMousePress(
    tool: Tool,
    keyboardState: KeyboardState,
    labelerConf: LabelerConf
) {
    when (tool) {
        Tool.Cursor -> handleCursorPress(keyboardState, labelerConf)
        Tool.Scissors -> Unit
    }
}

private fun MutableState<MarkerCursorState>.handleCursorPress(
    keyboardState: KeyboardState,
    labelerConf: LabelerConf
) {
    if (!keyboardState.isCtrlPressed) {
        if (value.mouse == Mouse.Hovering) {
            val lockedDragByBaseField =
                labelerConf.lockedDrag.useDragBase &&
                    labelerConf.fields.getOrNull(value.pointIndex)?.dragBase == true
            val lockedDragByStart =
                labelerConf.lockedDrag.useStart && value.usingStartPoint
            val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor keyboardState.isShiftPressed
            update { startDragging(lockedDrag) }
        }
    }
}

private fun MarkerState.handleMouseRelease(
    tool: Tool,
    event: PointerEvent,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    cutEntry: (Int, Float) -> Unit,
    keyboardState: KeyboardState
) {
    if (keyboardState.isCtrlPressed) {
        val x = event.changes.first().position.x
        val clickedRange = getClickedAudioRange(x, leftBorder, rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: canvasParams.dataLength.toFloat()
            playSampleSection(start, end)
        }
    } else {
        when (tool) {
            Tool.Cursor -> handleCursorRelease(submitEntry)
            Tool.Scissors -> handleScissorsRelease(cutEntry)
        }
    }
}

private fun MarkerState.handleCursorRelease(submitEntry: () -> Unit) {
    submitEntry()
    cursorState.update { finishDragging() }
}

private fun MarkerState.handleScissorsRelease(
    cutEntry: (Int, Float) -> Unit
) {
    val scissorsState = scissorsState

    val position = scissorsState.requireValue().position
    if (position != null) {
        val timePosition = entryConverter.convertToMillis(position)
        val entryIndex = getEntryIndexByCutPosition(position)
        cutEntry(entryIndex, timePosition)
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    entriesInPixel: List<EntryInPixel>,
    currentIndex: Int,
    canvasLength: Int,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel
) {
    LaunchedEffect(currentIndex, canvasLength, horizontalScrollState.maxValue) {
        scrollFitViewModel.update(horizontalScrollState, canvasLength, entriesInPixel, currentIndex)
    }
}
