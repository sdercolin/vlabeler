@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.canMoveParameter
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
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.clear
import com.sdercolin.vlabeler.util.contains
import com.sdercolin.vlabeler.util.getScreenRange
import com.sdercolin.vlabeler.util.length
import com.sdercolin.vlabeler.util.requireValue
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

private const val RegionAlpha = 0.3f
private val EditableOutsideRegionColor = White
private const val UneditableRegionAlpha = 0.9f
private val UneditableRegionColor = Black
const val IdleLineAlpha = 0.7f
private const val StrokeWidth = 2f
val LabelSize = DpSize(40.dp, 25.dp)
val LabelShiftUp = 8.dp
private const val LabelMaxChunkLength = 5000

@Composable
fun MarkerPointEventContainer(
    screenRange: FloatRange?,
    keyboardState: KeyboardState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState,
    content: @Composable BoxScope.() -> Unit
) {
    val tool = editorState.tool
    val playByCursor = remember(appState.appConf.playback.playOnDragging, appState.player) {
        if (appState.appConf.playback.playOnDragging.enabled) {
            { appState.player.playByCursor(it) }
        } else {
            { _: Float -> }
        }
    }
    val playSection = remember(appState.player) {
        { start: Float, end: Float -> appState.player.playSection(start, end) }
    }
    Box(
        modifier = Modifier.fillMaxSize()
            .onPointerEvent(PointerEventType.Move) { event ->
                if (tool == Tool.Cursor &&
                    state.cursorState.value.mouse == MarkerCursorState.Mouse.Dragging &&
                    event.buttons.areAnyPressed.not()
                ) {
                    state.handleMouseRelease(
                        tool,
                        event,
                        editorState::submitEntries,
                        playSection,
                        editorState::cutEntry,
                        keyboardState,
                        screenRange
                    )
                    return@onPointerEvent
                }
                state.handleMouseMove(
                    tool,
                    event,
                    editorState::updateEntries,
                    screenRange,
                    playByCursor
                )
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                state.cursorState.handleMousePress(tool, keyboardState, event, state.labelerConf)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                state.handleMouseRelease(
                    tool,
                    event,
                    editorState::submitEntries,
                    appState.player::playSection,
                    editorState::cutEntry,
                    keyboardState,
                    screenRange
                )
            },
        content = content
    )
}

@Composable
fun MarkerCanvas(
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState
) {
    FieldBorderCanvas(horizontalScrollState, state, appState)
    LaunchAdjustScrollPosition(
        state.entriesInPixel,
        editorState.project.currentIndex,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel
    )
    LaunchedEffect(horizontalScrollState.maxValue, horizontalScrollState.value) {
        editorState.scrollOnResolutionChangeViewModel.scroll(horizontalScrollState)
    }
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
fun MarkerLabels(
    screenRange: FloatRange?,
    appState: AppState,
    state: MarkerState
) {
    val requestRename: (Int) -> Unit = remember(appState) {
        { appState.openEditEntryNameDialog(it, InputEntryNameDialogPurpose.Rename) }
    }
    val jumpToEntry: (Int) -> Unit = remember(appState) {
        { appState.jumpToEntry(it) }
    }
    val onHovered: (Int, Boolean) -> Unit = remember(state) {
        { index, hovered -> state.onLabelHovered(index, hovered) }
    }

    val chunkCount = ceil(state.canvasParams.lengthInPixel.toFloat() / LabelMaxChunkLength).toInt()
    val chunkLength = state.canvasParams.lengthInPixel.toFloat() / chunkCount
    val chunkLengthDp = state.canvasParams.canvasWidthInDp / chunkCount

    val chunkVisibleList = List(chunkCount) {
        val range = (it * chunkLength)..((it + 1) * chunkLength)
        screenRange?.contains(range) == true
    }
    FieldLabels(state, chunkCount, chunkLength, chunkLengthDp, chunkVisibleList)
    if (state.labelerConf.continuous) {
        NameLabels(
            state,
            requestRename,
            jumpToEntry,
            onHovered,
            chunkCount,
            chunkLength,
            chunkLengthDp,
            chunkVisibleList
        )
    }
}

@Composable
private fun FieldBorderCanvas(
    horizontalScrollState: ScrollState,
    state: MarkerState,
    appState: AppState
) {
    val screenRange = horizontalScrollState.getScreenRange(state.canvasParams.lengthInPixel)

    Canvas(Modifier.fillMaxSize()) {
        screenRange ?: return@Canvas
        try {
            val entriesInPixel = state.entriesInPixel
            val start = entriesInPixel.first().start
            val end = entriesInPixel.last().end
            val canvasActualWidth = state.canvasParams.lengthInPixel.toFloat()
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val cursorState = state.cursorState
            val labelerConf = state.labelerConf
            state.canvasHeightState.value = canvasHeight

            // Draw left border
            if (leftBorder >= 0 && (0f..leftBorder in screenRange)) {
                val leftBorderColor = UneditableRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                drawRect(
                    color = leftBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset.Zero,
                    size = Size(width = relativeLeftBorder, height = canvasHeight)
                )
                drawLine(
                    color = leftBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeLeftBorder, 0f),
                    end = Offset(relativeLeftBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw start
            if (leftBorder..start in screenRange) {
                val startColor = EditableOutsideRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                val relativeStart = start - screenRange.start
                val coercedLeftBorder = relativeLeftBorder.coerceAtLeast(0f)
                val coercedStart = relativeStart.coerceAtMost(screenRange.length)
                drawRect(
                    color = startColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedLeftBorder, 0f),
                    size = Size(width = coercedStart - coercedLeftBorder, height = canvasHeight)
                )
                val startLineAlpha = if (cursorState.value.usingStartPoint) 1f else IdleLineAlpha
                drawLine(
                    color = startColor.copy(alpha = startLineAlpha),
                    start = Offset(coercedStart, 0f),
                    end = Offset(coercedStart, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw custom fields and borders
            for (entryIndex in state.entriesInPixel.indices) {
                val entryInPixel = state.entriesInPixel[entryIndex]
                if (entryIndex != 0) {
                    val border = state.entryBorders[entryIndex - 1]
                    val borderColor = EditableOutsideRegionColor
                    val pointIndex = cursorState.value.pointIndex
                    val borderLineAlpha =
                        if (state.isBorderIndex(pointIndex) &&
                            state.getEntryIndexesByBorderIndex(pointIndex).second == entryIndex
                        ) 1f else IdleLineAlpha
                    if (border in screenRange) {
                        val relativeBorder = border - screenRange.start
                        drawLine(
                            color = borderColor.copy(alpha = borderLineAlpha),
                            start = Offset(relativeBorder, 0f),
                            end = Offset(relativeBorder, canvasHeight),
                            strokeWidth = StrokeWidth
                        )
                    }
                }
                for (fieldIndex in labelerConf.fields.indices) {
                    val field = labelerConf.fields[fieldIndex]
                    val x = entryInPixel.points[fieldIndex]
                    val waveformsHeight = canvasHeight * state.waveformsHeightRatio
                    val height = waveformsHeight * field.height
                    val top = waveformsHeight - height
                    val color = field.color.toColor()
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
                        val right = left + width
                        if (left..right in screenRange) {
                            val coercedRelativeLeft = (left - screenRange.start).coerceAtLeast(0f)
                            val coercedRelativeRight = (right - screenRange.start).coerceAtMost(screenRange.length)
                            drawRect(
                                color = color,
                                alpha = RegionAlpha,
                                topLeft = Offset(coercedRelativeLeft, top),
                                size = Size(width = coercedRelativeRight - coercedRelativeLeft, height = height)
                            )
                        }
                    }

                    val pointIndex = fieldIndex + entryIndex * (state.labelerConf.fields.size + 1)
                    val lineAlpha = if (cursorState.value.pointIndex != pointIndex) IdleLineAlpha else 1f
                    if (x in screenRange) {
                        val relativeX = x - screenRange.start
                        drawLine(
                            color = color.copy(alpha = lineAlpha),
                            start = Offset(relativeX, top),
                            end = Offset(relativeX, canvasHeight),
                            strokeWidth = StrokeWidth
                        )
                    }
                }
            }

            // Draw end
            if (end..rightBorder in screenRange) {
                val endColor = EditableOutsideRegionColor
                val relativeEnd = end - screenRange.start
                val relativeRightBorder = rightBorder - screenRange.start
                val coercedEnd = relativeEnd.coerceAtLeast(0f)
                val coercedRightBorder = relativeRightBorder.coerceAtMost(screenRange.length)
                drawRect(
                    color = endColor,
                    alpha = RegionAlpha,
                    topLeft = Offset(coercedEnd, 0f),
                    size = Size(width = coercedRightBorder - coercedEnd, height = canvasHeight)
                )
                val endLineAlpha = if (cursorState.value.usingEndPoint) 1f else IdleLineAlpha
                drawLine(
                    color = endColor.copy(alpha = endLineAlpha),
                    start = Offset(coercedEnd, 0f),
                    end = Offset(coercedEnd, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw right border
            if (rightBorder < canvasActualWidth && (rightBorder..canvasActualWidth in screenRange)) {
                val rightBorderColor = UneditableRegionColor
                val relativeRightBorder = rightBorder - screenRange.start
                drawRect(
                    color = rightBorderColor,
                    alpha = UneditableRegionAlpha,
                    topLeft = Offset(relativeRightBorder, 0f),
                    size = Size(width = screenRange.endInclusive - relativeRightBorder, height = canvasHeight)
                )
                drawLine(
                    color = rightBorderColor.copy(alpha = IdleLineAlpha),
                    start = Offset(relativeRightBorder, 0f),
                    end = Offset(relativeRightBorder, canvasHeight),
                    strokeWidth = StrokeWidth
                )
            }

            // Draw scissors
            state.scissorsState.value?.let { scissors ->
                val position = scissors.position
                if (scissors.disabled.not() && position != null && state.isValidCutPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = appState.appConf.editor.scissorsColor.toColor(),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = StrokeWidth * 2
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            if (isDebug) throw t
            Log.debug(t)
        }
    }
}

private fun MarkerState.handleMouseMove(
    tool: Tool,
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit,
    screenRange: FloatRange?,
    playByCursor: (Float) -> Unit
) {
    screenRange ?: return
    when (tool) {
        Tool.Cursor -> handleCursorMove(event, editEntries, screenRange, playByCursor)
        Tool.Scissors -> handleScissorsMove(event, screenRange)
    }
}

private fun MarkerState.handleCursorMove(
    event: PointerEvent,
    editEntries: (List<IndexedEntry>) -> Unit,
    screenRange: FloatRange,
    playByCursor: (Float) -> Unit
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x.coerceIn(0f, canvasParams.lengthInPixel.toFloat())
    val actualX = x + screenRange.start
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (cursorState.value.mouse == MarkerCursorState.Mouse.Dragging) {
        val updated = if (cursorState.value.lockedDrag) {
            getLockedDraggedEntries(cursorState.value.pointIndex, actualX, leftBorder, rightBorder)
        } else {
            getDraggedEntries(cursorState.value.pointIndex, actualX, leftBorder, rightBorder, labelerConf)
        }
        if (updated != entriesInPixel) {
            val updatedInMillis = updated.map { entryConverter.convertToMillis(it) }
            editEntries(updatedInMillis)
        }
        if (cursorState.value.previewOnDragging) {
            playByCursor(entryConverter.convertToFrame(actualX))
        }
    } else {
        val newPointIndex = getPointIndexForHovering(
            x = actualX,
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
    event: PointerEvent,
    screenRange: FloatRange
) {
    val scissorsState = scissorsState
    val x = event.changes.first().position.x + screenRange.start
    val position = x.takeIf { isValidCutPosition(it) }
    scissorsState.updateNonNull { copy(position = position) }
}

private fun MutableState<MarkerCursorState>.handleMousePress(
    tool: Tool,
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf
) {
    when (tool) {
        Tool.Cursor -> handleCursorPress(keyboardState, event, labelerConf)
        Tool.Scissors -> Unit
    }
}

private fun MutableState<MarkerCursorState>.handleCursorPress(
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf
) {
    val action = keyboardState.getEnabledMouseClickAction(event) ?: return
    if (action.canMoveParameter()) {
        if (value.mouse == MarkerCursorState.Mouse.Hovering) {
            val lockedDragByBaseField =
                labelerConf.lockedDrag.useDragBase &&
                    labelerConf.fields.getOrNull(value.pointIndex)?.dragBase == true
            val lockedDragByStart =
                labelerConf.lockedDrag.useStart && value.usingStartPoint
            val lockedDrag = (lockedDragByBaseField || lockedDragByStart) xor
                (action == MouseClickAction.MoveParameterInvertingPrimary)
            val withPreview = action == MouseClickAction.MoveParameterWithPlaybackPreview
            update { startDragging(lockedDrag, withPreview) }
        }
    }
}

private fun MarkerState.handleMouseRelease(
    tool: Tool,
    event: PointerEvent,
    submitEntry: () -> Unit,
    playSampleSection: (Float, Float) -> Unit,
    cutEntry: (Int, Float) -> Unit,
    keyboardState: KeyboardState,
    screenRange: FloatRange?
) {
    screenRange ?: return
    if (keyboardState.getEnabledMouseClickAction(event) == MouseClickAction.PlayAudioSection) {
        val x = event.changes.first().position.x
        val actualX = x + screenRange.start
        val clickedRange = getClickedAudioRange(actualX, leftBorder, rightBorder)
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
