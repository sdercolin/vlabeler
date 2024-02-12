package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.areAnyPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.audio.AudioSectionPlayer
import com.sdercolin.vlabeler.env.KeyboardState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.model.action.canMoveParameter
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.common.isLeftClick
import com.sdercolin.vlabeler.ui.common.isRightClick
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.END_POINT_INDEX
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.NONE_POINT_INDEX
import com.sdercolin.vlabeler.ui.editor.labeler.marker.MarkerCursorState.Companion.START_POINT_INDEX
import com.sdercolin.vlabeler.ui.theme.Black
import com.sdercolin.vlabeler.ui.theme.White
import com.sdercolin.vlabeler.util.FloatRange
import com.sdercolin.vlabeler.util.contains
import com.sdercolin.vlabeler.util.getNextOrNull
import com.sdercolin.vlabeler.util.getPreviousOrNull
import com.sdercolin.vlabeler.util.length
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.update
import com.sdercolin.vlabeler.util.updateNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

const val REGION_ALPHA = 0.3f
val EditableOutsideRegionColor = White
const val UNEDITABLE_REGION_ALPHA = 0.9f
val UneditableRegionColor = Black
const val IDLE_LINE_ALPHA = 0.7f
const val STROKE_WIDTH = 2f
val LabelSize = DpSize(45.dp, 25.dp)
val LabelShiftUp = 11.dp
const val LABEL_MAX_CHUNK_LENGTH = 5000

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MarkerPointEventContainer(
    screenRange: FloatRange?,
    keyboardState: KeyboardState,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState,
    content: @Composable BoxScope.() -> Unit,
) {
    val tool = editorState.tool
    val playByCursor = remember(appState.appConf.playback.playOnDragging, appState.player) {
        if (appState.appConf.playback.playOnDragging.enabled) {
            { appState.player.playByCursor(it) }
        } else {
            { _: Float -> }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
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
                        appState.player,
                        editorState::cutEntry,
                        keyboardState,
                        screenRange,
                        editorState.canUseOnScreenScissors,
                    )
                    return@onPointerEvent
                }
                state.handleMouseMove(
                    tool,
                    event,
                    editorState::updateEntries,
                    screenRange,
                    playByCursor,
                    horizontalScrollState,
                    coroutineScope,
                    editorState::commitEntryCut,
                    density,
                    appState.appConf,
                )
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                state.handleMousePress(tool, keyboardState, event, state.labelerConf, appState.appConf, screenRange)
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                state.handleMouseRelease(
                    tool,
                    event,
                    editorState::submitEntries,
                    appState.player,
                    editorState::cutEntry,
                    keyboardState,
                    screenRange,
                    editorState.canUseOnScreenScissors,
                )
            },
        content = content,
    )
}

@Composable
fun MarkerCanvas(
    canvasParams: CanvasParams,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorState: EditorState,
    appState: AppState,
) {
    FieldBorderCanvas(editorState, horizontalScrollState, state, appState.appConf.editor)
    LaunchAdjustScrollPosition(
        appState.appConf,
        state.entriesInPixel,
        editorState.project.currentModuleIndex,
        editorState.project.currentModule.currentIndex,
        canvasParams.lengthInPixel,
        horizontalScrollState,
        appState.scrollFitViewModel,
    )
    LaunchedEffect(editorState.tool) {
        state.switchTool(editorState.tool)
    }
    LaunchedEffect(editorState.keyboardViewModel, state) {
        editorState.keyboardViewModel.keyboardActionFlow.collectLatest {
            if (appState.isEditorActive.not()) return@collectLatest
            if (editorState.handleSetPropertyKeyAction(it)) return@collectLatest
            if (state.mayHandleScissorsKeyAction(
                    onScreenMode = editorState.canUseOnScreenScissors,
                    action = it,
                    cutEntry = editorState::cutEntry,
                )
            ) return@collectLatest
            val (updated, pointIndex) = state.getUpdatedEntriesByKeyAction(
                it,
                appState.appConf,
                editorState.project.labelerConf,
            ) ?: return@collectLatest
            state.editEntryIfNeeded(
                updated = updated,
                editEntries = editorState::submitEntries,
                method = Edition.Method.SetWithCursor,
                pointIndex = pointIndex,
            )
        }
    }
}

@Composable
fun MarkerLabels(
    screenRange: FloatRange?,
    appState: AppState,
    editorState: EditorState,
    state: MarkerState,
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

    val chunkCount = ceil(state.canvasParams.lengthInPixel / LABEL_MAX_CHUNK_LENGTH).toInt()
    val chunkLength = state.canvasParams.lengthInPixel / chunkCount

    val chunkVisibleList = List(chunkCount) {
        val range = (it * chunkLength)..((it + 1) * chunkLength)
        screenRange?.contains(range) == true
    }
    FieldLabels(state, chunkCount, chunkLength, chunkVisibleList)
    if (state.labelerConf.continuous) {
        NameLabels(
            appState.appConf,
            editorState,
            state,
            requestRename,
            jumpToEntry,
            onHovered,
            chunkCount,
            chunkLength,
            chunkVisibleList,
        )
    }
}

@Composable
private fun FieldBorderCanvas(
    editorState: EditorState,
    horizontalScrollState: ScrollState,
    state: MarkerState,
    editorConf: AppConf.Editor,
) {
    val screenRange = editorState.getScreenRange(state.canvasParams.lengthInPixel, horizontalScrollState)
    Canvas(Modifier.fillMaxSize()) {
        screenRange ?: return@Canvas
        try {
            val entriesInPixel = state.entriesInPixel
            val labelerConf = state.labelerConf
            val (start, startField) = if (labelerConf.useImplicitStart) {
                val fieldIndexToReplaceStart = labelerConf.fields.indexOfFirst { it.replaceStart }
                entriesInPixel.first().points[fieldIndexToReplaceStart] to labelerConf.fields[fieldIndexToReplaceStart]
            } else {
                entriesInPixel.first().start to null
            }
            val (end, endField) = if (labelerConf.useImplicitEnd) {
                val fieldIndexToReplaceEnd = labelerConf.fields.indexOfFirst { it.replaceEnd }
                entriesInPixel.last().points[fieldIndexToReplaceEnd] to labelerConf.fields[fieldIndexToReplaceEnd]
            } else {
                entriesInPixel.last().end to null
            }
            val canvasActualWidth = state.canvasParams.lengthInPixel
            val canvasHeight = size.height
            val leftBorder = state.leftBorder
            val rightBorder = state.rightBorder
            val cursorState = state.cursorState
            state.canvasHeightState.value = canvasHeight

            // Draw left border
            if (leftBorder >= 0 && (0f..leftBorder in screenRange)) {
                val leftBorderColor = UneditableRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                drawRect(
                    color = leftBorderColor,
                    alpha = UNEDITABLE_REGION_ALPHA,
                    topLeft = Offset.Zero,
                    size = Size(width = relativeLeftBorder, height = canvasHeight),
                )
                drawLine(
                    color = leftBorderColor.copy(alpha = IDLE_LINE_ALPHA),
                    start = Offset(relativeLeftBorder, 0f),
                    end = Offset(relativeLeftBorder, canvasHeight),
                    strokeWidth = STROKE_WIDTH,
                )
            }

            // Draw start
            if (leftBorder..start in screenRange) {
                val startColor = startField?.color?.toColor() ?: EditableOutsideRegionColor
                val relativeLeftBorder = leftBorder - screenRange.start
                val relativeStart = start - screenRange.start
                val coercedLeftBorder = relativeLeftBorder.coerceAtLeast(0f)
                val coercedStart = relativeStart.coerceAtMost(screenRange.length)
                drawRect(
                    color = startColor,
                    alpha = REGION_ALPHA,
                    topLeft = Offset(coercedLeftBorder, 0f),
                    size = Size(width = coercedStart - coercedLeftBorder, height = canvasHeight),
                )
                val startLineAlpha = if (cursorState.value.usingStartPoint) 1f else IDLE_LINE_ALPHA
                drawLine(
                    color = startColor.copy(alpha = startLineAlpha),
                    start = Offset(coercedStart, 0f),
                    end = Offset(coercedStart, canvasHeight),
                    strokeWidth = STROKE_WIDTH,
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
                        ) 1f else IDLE_LINE_ALPHA
                    if (border in screenRange) {
                        val relativeBorder = border - screenRange.start
                        drawLine(
                            color = borderColor.copy(alpha = borderLineAlpha),
                            start = Offset(relativeBorder, 0f),
                            end = Offset(relativeBorder, canvasHeight),
                            strokeWidth = STROKE_WIDTH,
                        )
                    }
                }
                for (fieldIndex in labelerConf.fields.indices) {
                    val field = labelerConf.fields[fieldIndex]
                    if (field.replaceStart || field.replaceEnd) continue
                    val x = entryInPixel.points[fieldIndex]
                    val waveformsHeight = canvasHeight * state.waveformsHeightRatio
                    val height = waveformsHeight * field.height
                    val top = waveformsHeight - height
                    val color = field.color.toColor()
                    val fillTargetIndex = when (field.filling) {
                        "start" -> START_POINT_INDEX
                        "end" -> END_POINT_INDEX
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
                                alpha = REGION_ALPHA,
                                topLeft = Offset(coercedRelativeLeft, top),
                                size = Size(width = coercedRelativeRight - coercedRelativeLeft, height = height),
                            )
                        }
                    }

                    val pointIndex = fieldIndex + entryIndex * (state.labelerConf.fields.size + 1)
                    val lineAlpha = if (cursorState.value.pointIndex != pointIndex) IDLE_LINE_ALPHA else 1f
                    if (x in screenRange) {
                        val relativeX = x - screenRange.start
                        drawLine(
                            color = color.copy(alpha = lineAlpha),
                            start = Offset(relativeX, top),
                            end = Offset(relativeX, canvasHeight),
                            strokeWidth = STROKE_WIDTH,
                        )
                    }
                }
            }

            // Draw end
            if (end..rightBorder in screenRange) {
                val endColor = endField?.color?.toColor() ?: EditableOutsideRegionColor
                val relativeEnd = end - screenRange.start
                val relativeRightBorder = rightBorder - screenRange.start
                val coercedEnd = relativeEnd.coerceAtLeast(0f)
                val coercedRightBorder = relativeRightBorder.coerceAtMost(screenRange.length)
                drawRect(
                    color = endColor,
                    alpha = REGION_ALPHA,
                    topLeft = Offset(coercedEnd, 0f),
                    size = Size(width = coercedRightBorder - coercedEnd, height = canvasHeight),
                )
                val endLineAlpha = if (cursorState.value.usingEndPoint) 1f else IDLE_LINE_ALPHA
                drawLine(
                    color = endColor.copy(alpha = endLineAlpha),
                    start = Offset(coercedEnd, 0f),
                    end = Offset(coercedEnd, canvasHeight),
                    strokeWidth = STROKE_WIDTH,
                )
            }

            // Draw right border
            if (rightBorder < canvasActualWidth && (rightBorder..canvasActualWidth in screenRange)) {
                val rightBorderColor = UneditableRegionColor
                val relativeRightBorder = rightBorder - screenRange.start
                drawRect(
                    color = rightBorderColor,
                    alpha = UNEDITABLE_REGION_ALPHA,
                    topLeft = Offset(relativeRightBorder, 0f),
                    size = Size(width = screenRange.endInclusive - relativeRightBorder, height = canvasHeight),
                )
                drawLine(
                    color = rightBorderColor.copy(alpha = IDLE_LINE_ALPHA),
                    start = Offset(relativeRightBorder, 0f),
                    end = Offset(relativeRightBorder, canvasHeight),
                    strokeWidth = STROKE_WIDTH,
                )
            }

            // Draw scissors
            state.scissorsState.value?.let { scissors ->
                val position = scissors.position
                if (scissors.disabled.not() && position != null && state.isValidCutPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = editorConf.scissorsColor.toColor(),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = STROKE_WIDTH,
                        )
                    }
                }
            }

            // Draw playback cursor
            state.playbackState.value?.let { playbackState ->
                val position = playbackState.position
                if (position != null && state.isValidPlaybackPosition(position)) {
                    if (position in screenRange) {
                        val relativePosition = position - screenRange.start
                        drawLine(
                            color = editorConf.playerCursorColor.toColor(),
                            start = Offset(relativePosition, 0f),
                            end = Offset(relativePosition, canvasHeight),
                            strokeWidth = STROKE_WIDTH,
                        )
                    }
                }
            }

            // Draw playback range
            state.playbackState.value?.let { playbackState ->
                val startPosition = playbackState.draggingStartPosition ?: return@let
                val endPosition = playbackState.position ?: return@let
                if (startPosition in screenRange && endPosition in screenRange) {
                    val (relativeStartPosition, relativeEndPosition) = listOf(
                        startPosition - screenRange.start,
                        endPosition - screenRange.start,
                    ).sorted()
                    drawRect(
                        color = editorConf.playerCursorColor.toColor(),
                        alpha = REGION_ALPHA,
                        topLeft = Offset(relativeStartPosition, 0f),
                        size = Size(width = relativeEndPosition - relativeStartPosition, height = canvasHeight),
                    )
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
    editions: (List<Edition>) -> Unit,
    screenRange: FloatRange?,
    playByCursor: (Float) -> Unit,
    scrollState: ScrollState,
    scope: CoroutineScope,
    commitEntryCut: () -> Unit,
    density: Density,
    appConf: AppConf,
) {
    screenRange ?: return
    when (tool) {
        Tool.Cursor -> handleCursorMove(event, editions, screenRange, playByCursor, density)
        Tool.Scissors -> handleScissorsMove(event, screenRange, commitEntryCut, density, appConf)
        Tool.Pan -> handlePanMove(event, scrollState, scope)
        Tool.Playback -> handlePlaybackMove(event, screenRange)
    }
}

private fun MarkerState.handleCursorMove(
    event: PointerEvent,
    editions: (List<Edition>) -> Unit,
    screenRange: FloatRange,
    playByCursor: (Float) -> Unit,
    density: Density,
) {
    val eventChange = event.changes.first()
    val x = eventChange.position.x
    val actualX = x + screenRange.start
    cursorState.update { copy(position = actualX) }
    val y = eventChange.position.y.coerceIn(0f, canvasHeightState.value.coerceAtLeast(0f))
    if (cursorState.value.mouse == MarkerCursorState.Mouse.Dragging) {
        val forcedDrag = cursorState.value.forcedDrag
        val updated = if (cursorState.value.lockedDrag) {
            getLockedDraggedEntries(cursorState.value.pointIndex, actualX, forcedDrag)
        } else {
            getDraggedEntries(cursorState.value.pointIndex, actualX, forcedDrag)
        }
        editEntryIfNeeded(
            updated = updated,
            editEntries = editions,
            method = Edition.Method.Dragging,
            pointIndex = cursorState.value.pointIndex,
        )
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
            density = density,
            labelSize = LabelSize,
            labelShiftUp = LabelShiftUp,
        )
        if (newPointIndex == NONE_POINT_INDEX) {
            cursorState.update { moveToNothing() }
        } else {
            cursorState.update { moveToHover(newPointIndex) }
        }
    }
}

private fun MarkerState.handleScissorsMove(
    event: PointerEvent,
    screenRange: FloatRange,
    commitEntryCut: () -> Unit,
    density: Density,
    appConf: AppConf,
) {
    val scissorsStateValue = scissorsState.value
    val x = event.changes.first().position.x + screenRange.start
    if (scissorsStateValue?.locked == true) {
        val lockedPosition = scissorsStateValue.position ?: return
        val distance = abs(x - lockedPosition)
        val threshold = with(density) { appConf.editor.scissorsSubmitThreshold.dp.toPx() }
        if (distance > threshold) {
            scissorsState.updateNonNull { copy(locked = false) }
            commitEntryCut()
        }
        return
    }
    val position = x.takeIf { isValidCutPosition(it) }
    scissorsState.updateNonNull { copy(position = position) }
}

private fun MarkerState.handlePanMove(
    event: PointerEvent,
    scrollState: ScrollState,
    scope: CoroutineScope,
) {
    if (panState.value?.isDragging != true) return
    val x = event.changes.first().positionChange().x
    scope.launch {
        scrollState.scrollBy(-x)
    }
}

private fun MarkerState.handlePlaybackMove(
    event: PointerEvent,
    screenRange: FloatRange,
) {
    val playbackState = playbackState
    val x = event.changes.first().position.x + screenRange.start
    val position = x.takeIf { isValidPlaybackPosition(it) }
    playbackState.updateNonNull { copy(position = position) }
}

private fun MarkerState.handleMousePress(
    tool: Tool,
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf,
    appConf: AppConf,
    screenRange: FloatRange?,
) {
    when (tool) {
        Tool.Cursor -> handleCursorPress(keyboardState, event, labelerConf, appConf)
        Tool.Scissors -> Unit
        Tool.Pan -> handlePanPress(event)
        Tool.Playback -> handlePlaybackPress(keyboardState, screenRange, event)
    }
}

private fun MarkerState.handleCursorPress(
    keyboardState: KeyboardState,
    event: PointerEvent,
    labelerConf: LabelerConf,
    appConf: AppConf,
) {
    val action = keyboardState.getEnabledMouseClickAction(event) ?: return
    if (action.canMoveParameter()) {
        val cursorStateValue = cursorState.value
        if (cursorStateValue.mouse == MarkerCursorState.Mouse.Hovering) {
            val invertLockedDrag = action == MouseClickAction.MoveParameterInvertingPrimary
            val lockedDrag = when (appConf.editor.lockedDrag) {
                AppConf.Editor.LockedDrag.UseLabeler -> {
                    val lockedDragByBaseField =
                        labelerConf.lockedDrag.useDragBase &&
                            labelerConf.fields.getOrNull(cursorStateValue.pointIndex)?.dragBase == true
                    val lockedDragByStart =
                        labelerConf.lockedDrag.useStart && cursorStateValue.usingStartPoint
                    lockedDragByBaseField || lockedDragByStart
                }
                AppConf.Editor.LockedDrag.UseStart -> cursorStateValue.usingStartPoint
                else -> false
            } xor invertLockedDrag
            val withPreview = action == MouseClickAction.MoveParameterWithPlaybackPreview
            val forcedDrag = action == MouseClickAction.MoveParameterIgnoringConstraints
            cursorState.update { startDragging(lockedDrag, withPreview, forcedDrag) }
        }
    }
}

private fun MarkerState.handlePanPress(event: PointerEvent) {
    if (event.isLeftClick) {
        panState.updateNonNull { copy(isDragging = true) }
    }
}

private fun MarkerState.handlePlaybackPress(
    keyboardState: KeyboardState,
    screenRange: FloatRange?,
    event: PointerEvent,
) {
    screenRange ?: return
    val action = keyboardState.getEnabledMouseClickAction(event, Tool.Playback) ?: return
    if (action in setOf(MouseClickAction.PlayAudioRange, MouseClickAction.PlayAudioRangeRepeat)) {
        val x = event.changes.first().position.x + screenRange.start
        val position = x.takeIf { isValidPlaybackPosition(it) } ?: return
        playbackState.updateNonNull { startDragging(position) }
    }
}

private fun MarkerState.handleMouseRelease(
    tool: Tool,
    event: PointerEvent,
    submitEntry: () -> Unit,
    audioSectionPlayer: AudioSectionPlayer,
    cutEntry: (Int, position: Float, pixelPosition: Float) -> Unit,
    keyboardState: KeyboardState,
    screenRange: FloatRange?,
    canUseOnScreenScissors: Boolean,
) {
    screenRange ?: return
    val caughtAction = keyboardState.getEnabledMouseClickAction(event)
    val handled = when (tool) {
        Tool.Cursor -> handleCursorRelease(submitEntry)
        Tool.Scissors -> handleScissorsRelease(canUseOnScreenScissors, cutEntry, event)
        Tool.Pan -> handlePanRelease()
        Tool.Playback -> {
            val playbackAction = keyboardState.getEnabledMouseClickAction(event, Tool.Playback)
            handlePlaybackRelease(audioSectionPlayer, screenRange, playbackAction)
        }
    }
    if (!handled && caughtAction == MouseClickAction.PlayAudioSection) {
        val x = event.changes.first().position.x
        val actualX = x + screenRange.start
        val clickedRange = getClickedAudioRange(actualX, leftBorder, rightBorder)
        if (clickedRange != null) {
            val start = clickedRange.first?.let { entryConverter.convertToFrame(it) } ?: 0f
            val end = clickedRange.second?.let { entryConverter.convertToFrame(it) }
                ?: canvasParams.dataLength.toFloat()
            audioSectionPlayer.playSection(start, end)
        }
    }
}

private fun MarkerState.handleCursorRelease(
    submitEntry: () -> Unit,
): Boolean = if (cursorState.value.mouse == MarkerCursorState.Mouse.Dragging) {
    cursorState.update { finishDragging() }
    submitEntry()
    true
} else {
    false
}

private fun MarkerState.handleScissorsRelease(
    onScreenMode: Boolean,
    cutEntry: (Int, position: Float, pixelPosition: Float) -> Unit,
    event: PointerEvent,
): Boolean {
    val scissorsState = scissorsState
    val position = scissorsState.value?.position ?: return false
    if (event.isRightClick) return false
    handleScissorsCut(onScreenMode, position, cutEntry)
    return true
}

private fun MarkerState.handlePanRelease(): Boolean {
    if (panState.value?.isDragging != true) return false
    panState.updateNonNull { copy(isDragging = false) }
    return true
}

private fun MarkerState.handlePlaybackRelease(
    audioSectionPlayer: AudioSectionPlayer,
    screenRange: FloatRange,
    action: MouseClickAction?,
): Boolean {
    val playbackState = playbackState
    val position = playbackState.value?.position ?: return false
    action ?: return true
    var startFrame = when (action) {
        MouseClickAction.PlayAudioFromStart -> 0f
        MouseClickAction.PlayAudioFromScreenStart -> entryConverter.convertToFrame(screenRange.start)
        MouseClickAction.PlayAudioUntilEnd,
        MouseClickAction.PlayAudioUntilScreenEnd,
        -> entryConverter.convertToFrame(position)
        MouseClickAction.PlayAudioRange,
        MouseClickAction.PlayAudioRangeRepeat,
        -> playbackState.value?.draggingStartPosition?.let { entryConverter.convertToFrame(it) } ?: return true
        else -> return true
    }
    var endFrame = when (action) {
        MouseClickAction.PlayAudioUntilEnd -> null
        MouseClickAction.PlayAudioUntilScreenEnd -> entryConverter.convertToFrame(screenRange.endInclusive)
        MouseClickAction.PlayAudioFromStart,
        MouseClickAction.PlayAudioFromScreenStart,
        MouseClickAction.PlayAudioRange,
        MouseClickAction.PlayAudioRangeRepeat,
        -> entryConverter.convertToFrame(position)
        else -> return true
    }
    if (endFrame != null && endFrame < startFrame) {
        val tmp = startFrame
        startFrame = endFrame
        endFrame = tmp
    }
    val repeat = action == MouseClickAction.PlayAudioRangeRepeat
    audioSectionPlayer.playSection(startFrame, endFrame, repeat)
    playbackState.updateNonNull { finishDragging() }
    return true
}

private fun MarkerState.mayHandleScissorsKeyAction(
    onScreenMode: Boolean,
    action: KeyAction,
    cutEntry: (Int, position: Float, pixelPosition: Float) -> Unit,
): Boolean {
    if (action != KeyAction.ScissorsCut) return false
    val position = scissorsState.value?.position ?: return false
    handleScissorsCut(onScreenMode, position, cutEntry)
    return true
}

private fun MarkerState.handleScissorsCut(
    onScreenMode: Boolean,
    position: Float,
    cutEntry: (Int, position: Float, pixelPosition: Float) -> Unit,
) {
    val timePosition = entryConverter.convertToMillis(position)
    val entryIndex = getEntryIndexByCutPosition(position)
    if (onScreenMode) {
        scissorsState.updateNonNull { copy(locked = true) }
    }
    cutEntry(entryIndex, timePosition, position)
}

private fun MarkerState.editEntryIfNeeded(
    updated: List<EntryInPixel>,
    editEntries: (List<Edition>) -> Unit,
    method: Edition.Method,
    pointIndex: Int,
) {
    if (updated != entriesInPixel) {
        val edited = updated - entriesInPixel.toSet()
        if (edited.isEmpty()) return

        fun getEditedFieldName(pointIndex: Int) = when (pointIndex) {
            START_POINT_INDEX -> "start"
            END_POINT_INDEX -> "end"
            else -> labelerConf.fields[pointIndex].name
        }

        val editions = edited.map { entry ->
            val entryInMillis = entryConverter.convertToMillis(entry)
            val entryIndexInEdited = updated.indexOfFirst { it.index == entry.index }
            val editedFieldName = getEditedFieldName(getPointIndexAsSingleEntry(entryIndexInEdited, pointIndex))
            Edition(
                entry.index,
                entryInMillis.entry,
                fieldNames = listOf(editedFieldName),
                method = method,
            )
        }.toMutableList()

        if (labelerConf.continuous) {
            val leftEntry = entriesInCurrentGroup.getPreviousOrNull { it.index == updated.firstOrNull()?.index }
            val firstEdited = edited.first()
            val firstOriginal = entriesInPixel.firstOrNull { it.index == firstEdited.index }
            val leftBorderEdited = firstEdited.start != firstOriginal?.start
            if (leftEntry != null && leftBorderEdited) {
                editions.add(
                    0,
                    Edition(
                        leftEntry.index,
                        leftEntry.entry.copy(end = editions.first().newValue.start),
                        fieldNames = listOf("end"),
                        method = method,
                    ),
                )
            }

            val rightEntry = entriesInCurrentGroup.getNextOrNull { it.index == updated.lastOrNull()?.index }
            val lastEdited = edited.last()
            val lastOriginal = entriesInPixel.lastOrNull { it.index == lastEdited.index }
            val rightBorderEdited = lastEdited.end != lastOriginal?.end
            if (rightEntry != null && rightBorderEdited) {
                editions.add(
                    Edition(
                        rightEntry.index,
                        rightEntry.entry.copy(start = editions.last().newValue.end),
                        fieldNames = listOf("start"),
                        method = method,
                    ),
                )
            }
        }
        editEntries(editions)
    }
}

@Composable
private fun LaunchAdjustScrollPosition(
    appConf: AppConf,
    entriesInPixel: List<EntryInPixel>,
    currentModuleIndex: Int,
    currentIndex: Int,
    canvasLength: Float,
    horizontalScrollState: ScrollState,
    scrollFitViewModel: ScrollFitViewModel,
) {
    val isLabelOnLeft = appConf.editor.continuousLabelNames.position.left
    LaunchedEffect(
        isLabelOnLeft,
        entriesInPixel,
        currentModuleIndex,
        currentIndex,
        canvasLength,
        horizontalScrollState.maxValue,
    ) {
        scrollFitViewModel.update(isLabelOnLeft, horizontalScrollState, canvasLength, entriesInPixel, currentIndex)
    }
}
